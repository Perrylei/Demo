package com.example.sysio.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class IOSocket {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9090, 20);
        while (true) {
            Socket accept = serverSocket.accept();
            new Thread(() -> {
                InputStream inputStream = null;
                try {
                    inputStream = accept.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    while (true) {
                        String s = reader.readLine();
                        if (s != null) {
                            System.out.println("客户端" + accept.getInetAddress() + "-" + accept.getPort() + "发送的消息：" + s);
                        } else {
                            accept.close();
                            break;
                        }
                    }
                    System.out.println("客户端断开！");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
