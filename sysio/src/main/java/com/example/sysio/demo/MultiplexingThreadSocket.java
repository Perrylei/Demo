package com.example.sysio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 多线程版本的多路复用器
 */
public class MultiplexingThreadSocket {

    private ServerSocketChannel serverSocketChannel = null;

    private Selector selector = null;

    public void initServer() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(9090));
            // 有三种模型是select,poll和epoll。默认选择epoll，但是可以根据启动参数-D修改。
            // 如果再epoll模型下，这一步open相当于，epoll_create -> fd3
            selector = Selector.open();
            /**
             * server在listen状态下的文件描述符fd4
             * register在三种多路复用器模型下的操作
             * select和poll: JVM种开辟一个数组将fd4添加进去
             * epoll: epoll_ctl(fd3, Add , fd4 EPOLLIN)
             */
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("服务器已启动。。。");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        initServer();
        try {
            while (true) {
                Set<SelectionKey> keys = selector.keys();
                System.out.println(keys.size() + " size");
                /**
                 * 调用多路复用器(select,poll和epoll)
                 *  select()具体再三种模型种的操作:
                 *    select -> select(fd4)
                 *    poll -> poll(fd4)
                 *    epoll -> epoll_wait()
                 *  TODO
                 *  这个select方法可以带一个参数timeout
                 *
                 *  懒加载：在触碰到selector.select()调用的时候触发了epoll_ctl的调用
                 */
                while (selector.select() > 0) {
                    // 返回有状态的fd集合
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    /**
                     * 不管你用三种多路复用器模型的哪一种，只能获取到状态，还是要一个个处理他们的R/W进行同步
                     * NIO的缺点是对每一个NIO的fd进行系统调用，浪费资源。这里调用了一次select犯法，就知道那些fd可以r/w了
                     * socket分为：listen（监听）和通信（R/W）
                     */
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        // 不移除会重复循环读取
                        keyIterator.remove();
                        if (key.isAcceptable()) {
                            // 接收一个新的连接
                            acceptHandle(key);
                        } else if (key.isReadable()) {
                            System.out.println("In 。。。");
                            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                            // 还是阻塞。即便是抛出线程去读取。但是在时差里，这个key的read事件会被重复触发
                            readHandle(key);
                        } else if (key.isWritable()) {
                            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                            writeHandle(key);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 写事件 -> 只要send-queue为空，就一定返回可以写的事件，就会回调我们的写方法
     * 写事件发生的前提条件：
     * 1.是否准备好写什么
     * 2.send-queue是否有空间
     * 3.在read的时候一开始就需要注册，但是在write的时候是依赖前面两条的，什么时候用什么时候注册（如果一开始就注册了写事件，就会进入死循环，一致被调用）
     *
     * @param key
     */
    private void writeHandle(SelectionKey key) {
        new Thread(() -> {
            System.out.println("Write handle...");
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            buffer.flip();
            try {
                while (buffer.hasRemaining()) {
                    client.write(buffer);
                }
                Thread.sleep(2000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            buffer.clear();
        }).start();
    }

    /**
     * 这个函数处理了读数据，顺带将写数据也一起处理了
     * 在当前线程，这个方法可能会阻塞，如果阻塞很长时间，其他IO早就歇菜了。所以提出了IO Threads
     * redis用了epoll，也有IO Threads的概念 TODO
     *
     * @param key
     */
    private void readHandle(SelectionKey key) {
        new Thread(() -> {
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            buffer.clear();
            int read = 0;
            try {
                while (true) {
                    read = client.read(buffer);
                    System.out.println(Thread.currentThread().getName() + "，" + client.getRemoteAddress() + "发送的数据：" + read);
                    if (read > 0) {
                        key.interestOps(SelectionKey.OP_READ);
                        client.register(key.selector(), SelectionKey.OP_WRITE, buffer);
                    } else if (read == 0) {
                        break;
                    } else {
                        client.close();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 这个函数是一个重点，处理了新连接进来的客户端
     * 根据之前的逻辑代码得到，accept得到新的连接并返回对应连接的fd
     * 该如何处理这个新的fd?
     * select和poll模型，由于它俩在内核中没有空间，那么在JVM种保存并且和前面的fd4的listen在一起
     * epoll则是通过epoll_ctl把新的客户端fd注册到内核空间
     *
     * @param key
     */
    private void acceptHandle(SelectionKey key) {
        try {
            ServerSocketChannel channel = (ServerSocketChannel) key.channel();
            // 调用accept接收客户端 fd7
            SocketChannel client = channel.accept();
            client.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
            /**
             * register在三种多路复用器模型中的操作
             *  select，poll: 在JVM种开辟了一个数组把fd7放进去
             *  epoll: epoll_ctl(fd3, add, fd7, EPOLLIN)
             */
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("建立了新的客户端连接" + client.getRemoteAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MultiplexingThreadSocket().start();
    }
}
