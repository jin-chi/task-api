package com.example.api;

import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class Main {
    public static void main(String[] args) throws Exception {

        // DB Driver の読み込み
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // "/tasks" で TasksController を登録
        server.createContext("/tasks", new TaskController());

        // サーバーをバックグラウンドで開始
        server.setExecutor(null);
        server.start();

        System.out.println("Server started on port 8080");
    }
}
