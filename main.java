import java.io.*;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.util.ArrayList;

public class main {

    public static void main(String[] args) throws IOException {
        int port = 80;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/static", new StaticFileHandler());
        server.createContext("/query", new QueryHandler());
        server.createContext("/deleteRows", new DeleteHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + port);
    }

    static class RootHandler implements HttpHandler {
    
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Define the path to the index file
            String indexFilePath = "html/index.html";
            byte[] response = Files.readAllBytes(Paths.get(indexFilePath));
            // Set the Content-Type header for HTML
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            // Create the response
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Get the requested path
            String requestPath = exchange.getRequestURI().getPath();
            String filePath = "html" + requestPath;
            // Check if the file exist before reading
            if(!Files.exists(Paths.get(filePath))) {
                System.out.println("File not found: " + filePath); // Debug print
                exchange.sendResponseHeaders(404, -1);
                return;
            } else {
                System.out.println("Loaded file: " + filePath);
            }
            // Load the file
            byte[] response = Files.readAllBytes(Paths.get(filePath));
            // Determine the MIME type
            String contentType = "application/octet-stream";
            if (filePath.endsWith(".js")) {
                contentType = "application/javascript";
            } else if (filePath.endsWith(".css")) {
                contentType = "text/css";
            } else if (filePath.endsWith(".html")) {
                contentType = "text/html; charset=UTF-8";
            }
            // Create the response
            exchange.getResponseHeaders().set("Content-type", contentType);
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }
    static class QueryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("POST".equals(exchange.getRequestMethod())) {
                    System.out.println("QueryHandler");
                    // Read the request body
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
                    BufferedReader reader = new BufferedReader(isr);
                    String recivedString = reader.lines().collect(Collectors.joining());
                    System.out.println("Recived string: " + recivedString); // Debug 
                    // Parse JSON into a map
                    Map<String, String> data = parseJsonToMap(recivedString);
                    // Execute query
                    String result = executeQuery(data.get("database"), data.get("username"), data.get("password"), data.get("query"));
                    
                    // Create response
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, result.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(result.getBytes());
                    os.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    exchange.sendResponseHeaders(500, -1);
                } catch (Exception ignored) {

                }
            }
        }
    }
    static class DeleteHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("POST".equals(exchange.getRequestMethod())) {
                    System.out.println("DeleteHandler");
                     // Read the request body
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
                    BufferedReader reader = new BufferedReader(isr);
                    String recivedString = reader.lines().collect(Collectors.joining());
                    System.out.println("Recieved string: " + recivedString);
                    // Parse JSON into map
                    Map<String, String> data = parseJsonToMap(recivedString);
                    // Execute delete request
                    deleteRow(data.get("database"), data.get("username"), data.get("password"), data.get("table"), data.get("column"), data.get("id"));
                    String select = "SELECT * FROM " + data.get("table") + " WHERE " + data.get("column") + " = " + data.get("id");
                    // Check if there is any data left
                    String result = executeQuery(data.get("database"), data.get("username"), data.get("password"), select);
                    // Create response
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, result.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(result.getBytes());
                    os.close();
                }
            } catch (Exception e){
                e.printStackTrace();
                try{
                    exchange.sendResponseHeaders(500, -1);
                } catch (Exception ignored){}
            }
           
        }
    }

    private static Map<String, String> parseJsonToMap(String json){
        Map<String, String> map = new HashMap<>();
        json = json.trim();

        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1); // Remove the outer curly braces
        }
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // Split key-value pairs
        for (String pair : pairs) {
            String[] keyValue = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2); // Split key and value
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("^\"|\"$", ""); // Remove quotes from key
                String value = keyValue[1].trim().replaceAll("^\"|\"$", ""); // Remove quotes from value
                map.put(key, value);
            }
        }
        return map;
    }
    private static String executeQuery(String database, String username, String password, String query){
        String url = "jdbc:mysql://localhost:3306/" + database;
        try (Connection conn = DriverManager.getConnection(url, username, password)){
            try (Statement stmt = conn.createStatement()){
                ResultSet resultSet = stmt.executeQuery(query);
                StringBuilder jsonBuilder = new StringBuilder();
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                // Append first bracket
                jsonBuilder.append("[\n");
                // Append rows
                boolean hasRows = false;
                boolean firstRow = true;
                // "while" is for each row
                while (resultSet.next()) {
                    hasRows = true;
                    if (!firstRow) {
                        jsonBuilder.append(",\n");
                    }
                    firstRow = false;
                    // Append first bracket of the row
                    jsonBuilder.append("{");
                    boolean firstColumn = true;
                    // "for" is for each column
                    for (int i = 1; i <= columnCount; i++) {
                        if (!firstColumn) {
                            jsonBuilder.append(", ");
                        }
                        firstColumn = false;
                        // Append the column name and value
                        jsonBuilder.append("\"").append(metaData.getColumnName(i)).append("\": ");
                        jsonBuilder.append("\"").append(resultSet.getString(i)).append("\"");
                    }
                    // Close the row
                    jsonBuilder.append("}");
                }

                if (!hasRows) {
                    jsonBuilder.append("\"Empty set\"");
                }

                jsonBuilder.append("\n]"); // Close the JSON array
                // Print the query and result for debugging
                System.out.println("Executing: " + query);
                System.out.println("Result: " + jsonBuilder.toString());
                return jsonBuilder.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "executeQuery";
    }
    private static void deleteRow(String database, String username, String password, String table, String column, String value){
        System.out.println("deleteData");
        String url = "jdbc:mysql://localhost:3306/" + database;
        String query = "DELETE FROM " + table + " WHERE " + column + " = " + value; 
        System.out.println("Query: " + query);
        try(Connection conn = DriverManager.getConnection(url, username, password)){
            try (Statement stmt = conn.createStatement()){
                stmt.executeUpdate(query);
                System.out.println("Executed: " + query);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}