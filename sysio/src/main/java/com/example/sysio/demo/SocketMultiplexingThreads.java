package com.example.sysio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketMultiplexingThreads {

    private ServerSocketChannel serverSocketChannel = null;

    private Selector selector1 = null;

    private Selector selector2 = null;

    private Selector selector3 = null;

    public void initServer() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(9090));
            selector1 = Selector.open();
            selector2 = Selector.open();
            selector3 = Selector.open();
            serverSocketChannel.register(selector1, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        SocketMultiplexingThreads multiplexingThreads = new SocketMultiplexingThreads();
        multiplexingThreads.initServer();
        NIOThread nioThread1 = new NIOThread(multiplexingThreads.selector1, 2);
        NIOThread nioThread2 = new NIOThread(multiplexingThreads.selector2);
        NIOThread nioThread3 = new NIOThread(multiplexingThreads.selector3);
        nioThread1.start();
        Thread.sleep(1000);
        nioThread2.start();
        nioThread3.start();
        System.out.println("服务器启动了");
        System.in.read();
    }
}

class NIOThread extends Thread {

    private Selector selector = null;

    static int selectors = 0;

    private int id = 0;

    volatile static BlockingQueue<SocketChannel>[] queue;

    static AtomicInteger idx = new AtomicInteger();

    public NIOThread(Selector selector) {
        this.selector = selector;
        id = idx.getAndIncrement() % selectors;
        System.out.println("Worker:" + id + "启动");
    }

    public NIOThread(Selector selector, int n) {
        this.selector = selector;
        this.selectors = n;
        queue = new LinkedBlockingQueue[selectors];
        for (int i = 0; i < n; i++) {
            queue[i] = new LinkedBlockingQueue<>();
        }
        System.out.println("Boss启动");
    }

    @Override
    public void run() {
        try {
            while (true) {
                while (selector.select(10) > 0) {
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isAcceptable()) {
                            acceptHandle(key);
                        } else if (key.isReadable()) {
                            readHandle(key);
                        }
                    }
                }
                if (!queue[id].isEmpty()) {
                    ByteBuffer buffer = ByteBuffer.allocate(8192);
                    SocketChannel client = queue[id].take();
                    client.register(selector, SelectionKey.OP_READ, buffer);
                    System.out.println("新的客户端：" + client.socket().getLocalAddress() + "-" + client.socket().getPort() + "分配到：" + id);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void acceptHandle(SelectionKey key) {
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel client = serverSocketChannel.accept();
            client.configureBlocking(false);
            int num = idx.getAndIncrement() % selectors;
            queue[num].add(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readHandle(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        int read = 0;
        try {
            while (true) {
                read = client.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
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
    }
}
