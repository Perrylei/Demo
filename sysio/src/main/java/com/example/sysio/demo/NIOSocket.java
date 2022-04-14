package com.example.sysio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

public class NIOSocket {

    public static void main(String[] args) throws IOException {
        List<SocketChannel> channelList = new LinkedList<>();
        // 服务端开启监听，接收客户端
        ServerSocketChannel socketChannel =  ServerSocketChannel.open();
        socketChannel.bind(new InetSocketAddress(9090));
        // OS设置为NOBLOCKING，作用：只接收客户端，不阻塞
        socketChannel.configureBlocking(false);
        while (true) {
            // 接收客户端不会阻塞，是由于上面设置了NOBLOCKING
            // accept 调用了内核  没有客户端连接进来的返回值是null（如果是BIO则会一直阻塞）
            // 如果有客户端连接， accept返回的是这个客户端的文件描述符(fd)
            SocketChannel channel = socketChannel.accept();
            if (channel != null) {
                // 重点 Socket（服务端的listen socket<连接请求三次握手后，往我这里扔，我去通过accept 得到  连接的socket>，连接socket<连接后的数据读写使用的> ）
                channel.configureBlocking(false);
                System.out.println(channel.socket().getInetAddress() + "-" + channel.socket().getPort() + "建立连接");
                channelList.add(channel);
            }
//            else {
//                System.out.println("打印：展现不阻塞");
//            }
            // 申请栈外空间
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4096);
            // 遍历建立连接的客户端判断是否接收到对应的发送数据，并打印
            for (SocketChannel s : channelList) {
                // 有三种结果类型 >0 -1 0
                int read = s.read(byteBuffer);
                if (read > 0) {
                    byteBuffer.flip();
                    byte[] bytes = new byte[byteBuffer.limit()];
                    byteBuffer.get(bytes);
                    String msg = new String(bytes);
                    System.out.println(s.socket().getInetAddress() + "-" + s.socket().getPort() + "收到的消息：" + msg);
                    byteBuffer.clear();
                }
            }
        }
    }

}
