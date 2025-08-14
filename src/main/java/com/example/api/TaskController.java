package com.example.api;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class TaskController implements HttpHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    // DB 接続情報
    private static final String DB_URL = "jdbc:mysql://localhost:3306/task_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        // GET リクエストの処理
        if ("GET".equalsIgnoreCase(method)) {
            handleGetTasks(exchange);
        } else {
            // GET 以外のリクエストは 405 Method Not Allowed を返す
            sendResponse(exchange, 405, "Method Not Allowed");
        }
    }

    private void handleGetTasks(HttpExchange exchange) throws IOException {
        try {
            // データベースからタスク一覧を取得
            List<Task> tasks = fetchAllTasksFromDatabase();

            // Java オブジェクトを JSON 文字列に変換
            String jsonResponse = objectMapper.writeValueAsString(tasks);

            // レスポンスを送信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 200, jsonResponse);
        } catch (SQLException e) {
            e.printStackTrace();
            // DB エラーが発生した場合は 500 Internal Server Error を返す
            sendResponse(exchange, 500, "{\"error\": \"Database error\"}");
        }
    }

    private List<Task> fetchAllTasksFromDatabase() throws SQLException {
        List<Task> tasks = new ArrayList<>();
        // DB に接続
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, title, completed FROM tasks")) {

            // 結果セットをループし、Task オブジェクトに変換
            while (rs.next()) {
                Task task = new Task(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getBoolean("completed"));
                tasks.add(task);
            }
        }
        return tasks;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        exchange.sendResponseHeaders(statusCode, responseBody.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBody.getBytes());
        }
    }
}
