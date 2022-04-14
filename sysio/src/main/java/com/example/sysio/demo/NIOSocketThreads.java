package com.example.sysio.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * BIO 多线程的方式
 */
public class NIOSocketThreads {

    //server socket listen property:
    private static final boolean REUSE_ADDR = false;
    private static final int RECEIVE_BUFFER = 10;
    private static final int SO_TIMEOUT = 0;
    private static final int BACK_LOG = 2;

    //client socket listen property on server endpoint:
    private static final boolean CLI_REUSE_ADDR = false;
    private static final boolean CLI_KEEPALIVE = false;
    private static final boolean CLI_NO_DELAY = false;
    private static final boolean CLI_LINGER = true;
    private static final boolean CLI_OOB = false;
    private static final int CLI_SEND_BUF = 20;
    private static final int CLI_REC_BUF = 20;
    private static final int CLI_LINGER_N = 0;
    private static final int CLI_TIMEOUT = 0;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;

        try {
            serverSocket  = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(9090), BACK_LOG);
            serverSocket.setReceiveBufferSize(RECEIVE_BUFFER);
            serverSocket.setReuseAddress(REUSE_ADDR);
            serverSocket.setSoTimeout(SO_TIMEOUT);
            System.out.println("server startup use port: 9090");
            while (true) {
                    Socket accept = serverSocket.accept();
                    System.out.println("接收到的客户" + accept.getInetAddress() + ":" + accept.getPort());
                    accept.setKeepAlive(CLI_KEEPALIVE);
                    accept.setOOBInline(CLI_OOB);
                    accept.setReceiveBufferSize(CLI_REC_BUF);
                    accept.setReuseAddress(CLI_REUSE_ADDR);
                    accept.setSendBufferSize(CLI_SEND_BUF);
                    accept.setSoLinger(CLI_LINGER, CLI_LINGER_N);
                    accept.setSoTimeout(CLI_TIMEOUT);
                    accept.setTcpNoDelay(CLI_NO_DELAY);
                    new Thread(() -> {
                        InputStream inputStream = null;
                        try {
                            inputStream = accept.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                            char[] data = new char[1024];
                            while (true) {
                                int num = reader.read(data);
                                if (num > 0) {
                                    System.out.println("客户端连接" + accept.getInetAddress() + ":" + accept.getPort() + "接收到数据：" + new String(data, 0 ,num));
                                } else if (num == 0) {
                                    System.out.println("客户端连接" + accept.getInetAddress() + ":" + accept.getPort() + "没有读取到数据");
                                } else {
                                    System.out.println("客户端连接" + accept.getInetAddress() + ":" + accept.getPort() + "已关闭");
                                    accept.close();
                                    break;
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            serverSocket.close();
        }
    }
}
