import java.io.*;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyStore.Entry;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class Main {

    public static void main(String[] args) throws IOException {
        int port = 80;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/static", new StaticFileHandler());
        server.createContext("/upload", new UploadHandler());
        server.createContext("/css", new StaticFileHandler());
        server.createContext("/img", new StaticFileHandler());
        server.createContext("/js", new StaticFileHandler());
        server.createContext("/query", new QueryHandler());
        server.createContext("/deleteRows", new DeleteHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/invite", new CheckInvite());
        server.createContext("/newuser", new NewUser());
        server.createContext("/questionForm", new QuestionForm());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + port);
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Get the headers
            Headers headers = exchange.getRequestHeaders();
            // Get the host
            String host = headers.getFirst("Host");
            System.out.println("Host: " + host);
            // If there is no host
            if (host == null) host = "unknown";
            
            String requestPath = exchange.getRequestURI().getPath();
            String[] requestSplit = requestPath.split("/");
            // Define the path to the index file
            String htmlFilePath = "www/static/html/index.html";
            // If no html file is specified target the index file
            String targetFile = "index.html";
            // Initial path
            String userPath;

            if (host.equalsIgnoreCase("norlund-johan-lukas.com")) {
                userPath = "www";
            } else if (host.equalsIgnoreCase("www.norlund-johan-lukas.com")) {
                userPath = "www";
            } else { 
                targetFile = "notfound.html";
                userPath = "www";
            }

            if (requestPath.endsWith(".html")) {
                targetFile = requestSplit[requestSplit.length - 1];
            }

            htmlFilePath = userPath + "/static/html/" + targetFile;

            System.out.println("Target file path: " + htmlFilePath);
            byte[] response = Files.readAllBytes(Paths.get(htmlFilePath));
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
            String[] requestPathSplits = requestPath.split("/");
            String fileName = requestPathSplits[requestPathSplits.length - 1];
            String contentType = "application/octet-stream";
            String filePath = "www" + requestPath;
            // Determine the MIME type and file path
            if (requestPath.endsWith(".js")) {
                filePath = "www/static/js/" + fileName;
                contentType = "application/javascript";
            } else if (requestPath.endsWith(".css")) {
                filePath = "www/static/css/" + fileName;
                contentType = "text/css";
            } else if (requestPath.endsWith(".html")) {
                filePath = "www/static/html/" + fileName;
                contentType = "text/html; charset=UTF-8";
            } else if (requestPath.endsWith(".gif") || requestPath.endsWith(".jpg")){
                filePath = "www/static/img/" + fileName;
            }
            // System.out.println("File path: " + filePath);
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
            // Create the response
            exchange.getResponseHeaders().set("Content-type", contentType);
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("POST".equals(exchange.getRequestMethod())) {
                    // Read the request body
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
                    BufferedReader reader = new BufferedReader(isr);
                    String recievedString = reader.lines().collect(Collectors.joining());
                    System.out.println("Recieved string: " + recievedString);
                    // Parse JSON into a map
                    Map<String, String> data = parseJsonToMap(recievedString);
                    Map<String, String> result = getUser(data.get("username"), data.get("password"));
                    String json;
                    // Check if the result is ok
                    if (result.get("exist").equals("yes")) {
                        // Parse result (Map<String, String>) into JSON
                        json = parseMapToJson(result);
                    } else {
                        json = "[{\"status\": \"failed\"}]";
                    }
                    System.out.println("Sending: " + json);
                    // Create response
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, json.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(json.getBytes());
                    os.close();
                } 
            } catch (Exception e){
                e.printStackTrace();
                try {
                    exchange.sendResponseHeaders(500, -1);
                } catch (Exception ignored){}
            }
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
    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                        System.out.println("Reciving upload...");
                        // Get the request headers
                        Headers requestHeaders = exchange.getRequestHeaders();
                        // Get the request content type
                        String requestContentType = requestHeaders.getFirst("Content-Type");
                        // Get the body in bytes
                        byte[] body = inputStreamToBytes(exchange.getRequestBody());
                        // Extract the headers from the body
                        String bodyHeaders = extractHeaders(body, requestContentType);
                        // Extract the body's content type
                        String bodyContentType = extractContentType(bodyHeaders);
                        System.out.println("bodyContentType: " + bodyContentType);
                        String response = "";
                        
                        handleMultipartFormData(body, requestContentType);
                        response = "File uploaded successfully";
                    
                        exchange.sendResponseHeaders(200, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    } catch (Exception e){
                        e.printStackTrace();
                        String response = "Upload failed: " + e.getMessage();
                        exchange.sendResponseHeaders(500, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
    static class CheckInvite implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if("POST".equals(exchange.getRequestMethod())){
                // Read request body
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String recivedString = reader.lines().collect(Collectors.joining());
                System.out.println("Recieved string: " + recivedString);
                // Parse JSON into map
                Map<String, String> map = parseJsonToMap(recivedString);
                // Check if the invite exsist in the database
                String result = getInvite(map.get("invite"));
                String response = "[{\"status\": \"fail\"}]";
                if (result.equals("ok")) {
                    response = "[{\"status\": \"ok\"}]";
                }
                // Create exchange
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                System.out.println("POST did not work");
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
    static class NewUser implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if("POST".equals(exchange.getRequestMethod())){
                // Get the request body
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String recievedString = reader.lines().collect(Collectors.joining());
                // Parse JSON into map
                Map<String, String> map = parseJsonToMap(recievedString);
                // Insert new user 
                insertRow("webserver", "lukas", "Tvt!77@ren", "users", map);
                // Create the user's database
                createNewUser("lukas", "Tvt!77@ren", map.get("name"), map.get("password"));
                // Create exchange
                String response = "[{\"status\": \"ok\"}]";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
    static class QuestionForm implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if("POST".equals(exchange.getRequestMethod())){
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String request = reader.lines().collect(Collectors.joining());
                System.out.println("Request: " + request);
                // Parse JSON into map
                Map<String, String> map = parseJsonToMap(request);
                // Insert the message into the database
                String insert = insertMessage(map.get("name"), map.get("email"), map.get("content"));
                String response = "[{\"status\": \"fail\"}]";
                if (insert.equals("ok")) {
                    response = "[{\"status\": \"ok\"}]";
                }
                // Create exchange
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
    
    private static String insertMessage(String name, String email, String message){
        String url = "jdbc:mysql://localhost:3306/webserver";
        try (Connection conn = DriverManager.getConnection(url, "lukas", "Tvt!77@ren")){
            String sql = "INSERT INTO messages (name, email, message) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, message);
            pstmt.execute();
            return "ok";
        } catch (Exception e){
            e.printStackTrace();
            return "fail";
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
    private static String parseMapToJson(Map<String, String> map){
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[{");
        boolean firstValue = true;
        // Loop through the map and extract key and value
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!firstValue) {
                jsonBuilder.append(", ");
            }
            jsonBuilder.append("\"" + entry.getKey() + "\": ");
            jsonBuilder.append("\"" + entry.getValue() + "\"");
            firstValue = false;
            // String key = entry.getKey();
            // String value = entry.getValue();
            // System.out.println("Key: " + key + ", Value: " + value);
        }
        jsonBuilder.append("}]");
        System.out.println("JSON: " + jsonBuilder.toString());
        return jsonBuilder.toString();
    }
    private static Map<String, String> getUser(String username, String password){
        String url = "jdbc:mysql://localhost:3306/webserver";
        try(Connection conn = DriverManager.getConnection(url, "lukas", "Tvt!77@ren")){
            String sql = "SELECT * FROM users WHERE name = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pstmt.setString(1, username);
            ResultSet resultSet = pstmt.executeQuery();
            Map<String, String> user = new HashMap<>();
            while (resultSet.next()){
                if (resultSet.getString("name").equals(username) && 
                    resultSet.getString("password").equals(password)) {
                    user.put("exist", "yes");
                    user.put("id", resultSet.getString("id"));
                    user.put("name", resultSet.getString("name"));
                    user.put("password", resultSet.getString("password"));
                    user.put("domain", resultSet.getString("domain"));
                    user.put("email", resultSet.getString("email"));
                    user.put("phone", resultSet.getString("phone"));
                    return user;
                }
            }
            user.put("exist", "no");
            return user;
            
        }catch (Exception e){
            e.printStackTrace();
        }
        Map<String, String> user = new HashMap<>();
        user.put("exist", "no");
        return user;
    }
    private static String getInvite(String invite){
        String url = "jdbc:mysql://localhost:3306/webserver";
        try (Connection conn = DriverManager.getConnection(url, "lukas", "Tvt!77@ren")){
            String sql = "SELECT * FROM invites WHERE invite = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pstmt.setString(1, invite);
            ResultSet resultSet = pstmt.executeQuery();
            int count = 0;
            while (resultSet.next()) {
                count++;
                if (count > 1) {
                    return "fail";
                }
            }
            if (count == 0) {
                return "fail";
            }
            return "ok";
        } catch (Exception e){
            e.printStackTrace();
        }
        return "fail";
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
    private static String resultSetToString(ResultSet resultSet) throws SQLException{
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
        return jsonBuilder.toString();
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
    private static void insertRow(String database, String username, String password, String table, Map<String, String> sql){
        String url = "jdbc:mysql://localhost:3306/" + database;
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append("INSERT INTO ").append(table).append(" (");
        // Append keys to the sql string
        boolean firstKey = true;
        for (String key : sql.keySet()){
            if (!firstKey) {
                sqlBuild.append(", ").append(key);
            } else {
                sqlBuild.append(key);
                firstKey = false;
            }
        }
        sqlBuild.append(") VALUES (");
        // Append the values
        boolean firstValue = true;
        for (String key : sql.keySet()){
            String value = sql.get(key);
            if (!firstValue) {
                sqlBuild.append(", ");
            } else {
                firstValue = false;
            }
            sqlBuild.append("'").append(value).append("'");
        }
        sqlBuild.append(")");
        System.out.println(sqlBuild.toString());
        // Insert the row
        try (Connection conn = DriverManager.getConnection(url, username, password)){
            try (Statement stmt = conn.createStatement()){
                stmt.executeUpdate(sqlBuild.toString());
                System.out.println("Executed: " + sqlBuild.toString());
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    private static void createNewUser(String username, String password, String newuser, String newpassword){
        // Build the statements
        StringBuilder createUser = new StringBuilder();
        createUser.append("CREATE USER '").append(newuser).append("'@'localhost' IDENTIFIED BY '").append(newpassword).append("'");
        String createDatabase = "CREATE DATABASE " + newuser + "_db";
        StringBuilder privilages = new StringBuilder();
        privilages.append("GRANT ALL PRIVILEGES ON ").append(newuser).append("_db").append(".* TO '").append(newuser).append("'@'localhost' WITH GRANT OPTION");
        // Start the connection and execute the statements
        String url = "jdbc:mysql://localhost:3306/";
        try (Connection conn = DriverManager.getConnection(url, username, password)){
            try (Statement stmt = conn.createStatement()){
                // Create the user
                stmt.executeUpdate(createUser.toString());
                // Create database
                stmt.executeUpdate(createDatabase);
                // Grant privileges
                stmt.executeUpdate(privilages.toString());
                // Flush privileges
                stmt.executeUpdate("FLUSH PRIVILEGES");
            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }
    private static byte[] inputStreamToBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int bytesRead;
        while((bytesRead = is.read(data)) != -1){
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
    private static void handleMultipartFormData(byte[] body, String contentType) throws IOException {
        String boundary = contentType.split("boundary=")[1];
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        byte[] closingBoundaryBytes = ("--" + boundary + "--").getBytes(StandardCharsets.UTF_8);

        int pos = 0;
        while (true) {
            // Find next boundary
            int boundaryIndex = indexOf(body, boundaryBytes, pos);
            if (boundaryIndex == -1) break;

            // Check if it's the closing boundary
            if (startsWith(body, boundaryIndex, closingBoundaryBytes)) {
                break; // Done
            }

            // Move to content after boundary and CRLF
            pos = boundaryIndex + boundaryBytes.length;
            if (pos + 1 < body.length && body[pos] == '\r' && body[pos + 1] == '\n') {
                pos += 2;
            }

            // Find the start of the next boundary (i.e., end of this part)
            int nextBoundary = indexOf(body, boundaryBytes, pos);
            if (nextBoundary == -1) break;

            byte[] part = Arrays.copyOfRange(body, pos, nextBoundary);

            // Trim trailing newlines
            while (part.length > 0 && (part[part.length - 1] == '\n' || part[part.length - 1] == '\r')) {
                part = Arrays.copyOf(part, part.length - 1);
            }

            String preview = new String(part, 0, Math.min(part.length, 200), StandardCharsets.UTF_8);
            System.out.println("Part preview:\n" + preview);

            int headerEnd = indexOf(part, "\r\n\r\n".getBytes(StandardCharsets.UTF_8), 0);
            if (headerEnd == -1) {
                System.err.println("Failed to find header/content separator in part.");
                continue; // Skip this part to avoid crashing
            }

            String headers = new String(part, 0, headerEnd, StandardCharsets.UTF_8);
            byte[] fileContent = Arrays.copyOfRange(part, headerEnd + 4, part.length);

            if (headers.contains("filename=\"")) {
                String fileName = extractFileName(headers);
                String fileType = "img";
                if (fileName.endsWith(".html")) {
                    fileType = "html";
                } else if (fileName.endsWith(".css")) {
                    fileType = "css";
                } else if(fileName.endsWith(".js")){
                    fileType = "js";
                }
                if (fileName != null) {
                    saveFile(fileName, fileContent, fileType);
                }
            }

            pos = nextBoundary;
        }
    }
    private static void saveFile(String fileName, byte[] data, String path) throws IOException {
        // Path uploadDir = Paths.get("uploads");
        Path uploadDir = Paths.get("www", "static", path);
        
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        Path filePath = uploadDir.resolve(sanitizeFileName(fileName));
        Files.write(filePath, data);
        System.out.println("Saved file: " + filePath);
    }
    private static String extractHeaders(byte[] body, String contentType) throws IOException{
        String boundary = contentType.split("boundary=")[1];
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        int pos = 0;
        // Find next boundary
        int boundaryIndex = indexOf(body, boundaryBytes, pos);

        // Move to content after boundary and CRLF
        pos = boundaryIndex + boundaryBytes.length;

        // Find the start of the next boundary (i.e., end of this part)
        int nextBoundary = indexOf(body, boundaryBytes, pos);

        byte[] part = Arrays.copyOfRange(body, pos, nextBoundary);

        // Trim trailing newlines
        while (part.length > 0 && (part[part.length - 1] == '\n' || part[part.length - 1] == '\r')) {
            part = Arrays.copyOf(part, part.length - 1);
        }

        int headerEnd = indexOf(part, "\r\n\r\n".getBytes(StandardCharsets.UTF_8), 0);
            
        return new String(part, 0, headerEnd, StandardCharsets.UTF_8);
    }
    private static String extractContentType(String headers){
        return headers.split(":")[2].trim();
    }
    private static String extractDisposition(String headers){
        return headers.split(":")[1].trim();
    }
    private static String sanitizeFileName(String name){
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    private static String extractFileName(String headers) {
        int start = headers.indexOf("filename=\"") + 10;
        int end = headers.indexOf("\"", start);
        if (start < 10 || end <= start) return null;
        return sanitizeFileName(headers.substring(start, end));
    }
    private static int indexOf(byte[] data, byte[] target, int from) {
        for (int i = from; i <= data.length - target.length; i++) {
            if (matchBytes(data, target, i)) return i;
        }
        return -1;
    }
    private static boolean matchBytes(byte[] data, byte[] target, int offset) {
        if (offset + target.length > data.length) return false;
        for (int i = 0; i < target.length; i++) {
            if (data[offset + i] != target[i]) return false;
        }
        return true;
    }
    private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
        return matchBytes(data, prefix, offset);
    }
}
