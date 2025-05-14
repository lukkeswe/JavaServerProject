import java.io.*;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class main {

    public static void main(String[] args) throws IOException {
        int port = 80;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/static", new StaticFileHandler());
        server.createContext("/databasemanager", new DatabaseHandler());
        server.createContext("/fileUpload", new FileUploadPageHandler());
        server.createContext("/upload", new UploadHandler());
        server.createContext("/css", new StaticFileHandler());
        server.createContext("/img", new StaticFileHandler());
        server.createContext("/js", new StaticFileHandler());
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
            String indexFilePath = "www/static/html/index.html";
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
            String filePath = "www" + requestPath;
            if (filePath.endsWith(".html")){
                filePath = "www/static/html" + requestPath;
                System.out.println("Requesting: " + filePath);
            }
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
    static class DatabaseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Define the path to the html file
            String htmlFilePath = "www/static/html/databasemanager.html";
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
    static class FileUploadPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Define the path to the html file
            String htmlFilePath = "www/static/html/fileupload.html";
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