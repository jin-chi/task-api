package com.example.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
        } else if ("POST".equals(method)) {
            handlePostTask(exchange);
        } else if ("PUT".equalsIgnoreCase(method)) {
            handlePutTask(exchange);
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

    private void handlePostTask(HttpExchange exchange) throws IOException {
        try (InputStream requestBody = exchange.getRequestBody()) {
            // リクエストボディから JSON を読み込み、Task オブジェクトに変換
            Task newTask = objectMapper.readValue(requestBody, Task.class);

            // データベースに新しいタスクを保存
            saveTaskToDatabase(newTask);

            // 201 Created ステータスと成功メッセージを返す
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 201, "{\"message\": \"Task added successfully\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\": \"Database error\"}");
        }
    }

    private void handlePutTask(HttpExchange exchange) throws IOException {
        try (InputStream requestBody = exchange.getRequestBody()) {
            String path = exchange.getRequestURI().getPath();
            int taskId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));

            Task updatedTask = objectMapper.readValue(requestBody, Task.class);

            updateTaskInDatabase(taskId, updatedTask);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 200, "{\"message\": \"Task updated successfully\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\": \"Database error\"}");
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, "{\"error\": \"Invalid task ID\"}");
        }
    }

    private void updateTaskInDatabase(int id, Task task) throws SQLException {
        String sql = "UPDATE tasks SET title = ?, completed = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, task.getTitle());
            pstmt.setBoolean(2, task.isCompleted());
            pstmt.setInt(3, id);
            pstmt.executeUpdate();
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
                tasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getBoolean("completed")));
            }
        }
        return tasks;
    }

    private void saveTaskToDatabase(Task task) throws SQLException {
        String sql = "INSERT INTO tasks (title) VALUES (?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, task.getTitle());
            pstmt.executeUpdate();
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] responseBytes = responseBody.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
