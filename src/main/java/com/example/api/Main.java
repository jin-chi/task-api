package com.example.api;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

public class Main {
    public static void main(String[] args) throws Exception {
        
        // ポート 8080 でサーバーを起動
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // ルートパスに "/" ハンドラーを登録
        server.createContext("/", exchange -> {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> data = Collections.singletonMap("message", "Hello, API world!");
            String jsonResponse = objectMapper.writeValueAsString(data);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes());
            }
        });

        // サーバーをバックグラウンドで開始
        server.setExecutor(null);
        server.start();

        System.out.println("Server started on port 8080");
    }
}
