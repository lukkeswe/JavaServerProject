import java.io.*;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.*;
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
        server.createContext("/createTable", new CreateNewTable());
        server.createContext("/game", new GameAssetsHandler());
        server.createContext("/listAllFiles", new ListAllFiles());
        server.createContext("/deleteFile", new DeleteFileHandler());
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
            // If there is no host
            if (host == null) host = "unknown";
            
            String requestPath = exchange.getRequestURI().getPath();
            String[] requestSplit = requestPath.split("/");
            // Define the path to the index file
            String htmlFilePath = "/home/lukas/JavaServerProject/www/static/html/index.html";
            // If no html file is specified target the index file
            String targetFile = "index.html";
            // Initial path
            String userPath = DomainsConfig.domainMap.getOrDefault(host, null);
            if (userPath == null) {
                userPath = "/home/lukas/JavaServerProject/www";
                targetFile = "notfound.html";
            }
            // Initial response
            byte[] response = null;
            // If the requested file is a html file
            if (requestPath.endsWith(".html")) {
                targetFile = requestSplit[requestSplit.length - 1];
            } else if (requestPath.endsWith(".php")){
                // If the requested file is a php file
                // Check if the file exist
                Path phpPath = Paths.get(userPath + "/static/php/" + requestSplit[requestSplit.length - 1]);
                if (!Files.exists(phpPath) || !Files.isRegularFile(phpPath)) {
                    response = Files.readAllBytes(Paths.get("www/static/html/notfound.html"));
                } else {
                    String phpFilePath = userPath + "/static/php/" + requestSplit[requestSplit.length - 1];
                    // Process the file with PHP if it is a PHP file
                    String username = PhpConfig.phpMap.getOrDefault(host, null);
                    String phpIniPath;
                    boolean norlundJohanLukas = false;
                    if (host.equals("norlund-johan-lukas.com") || host.equals("norlund-johan-lukas.com")){
                        phpIniPath = "www/static/php/lukas.ini"; 
                        norlundJohanLukas = true;
                    } else phpIniPath = "/etc/php/users/" + username + ".ini";
                    // Create the ProcessBuilder with a "-c" flag
                    ProcessBuilder pb = new ProcessBuilder("php-cgi", "-c", phpIniPath);
                    // Set working directory
                    if (norlundJohanLukas) {
                        pb.directory(new File("www/static/php/"));
                    } else pb.directory(new File(userPath + "/static/php/"));
                    //Copy headers from the request
                    Map<String, String> env = pb.environment();
                    // Set common CGI variables
                    env.put("SCRIPT_FILENAME", phpFilePath);
                    env.put("REQUEST_METHOD", exchange.getRequestMethod());
                    env.put("REDIRECT_STATUS", "200");
                    // Handle GET
                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        URI uri = exchange.getRequestURI();
                        String query = uri.getRawQuery(); // Encoded query string
                        if (query != null) {
                            env.put("QUERY_STRING", query);
                        }
                    }
                    //Handle POST
                    byte[] postData = null;
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        Headers reqHeaders = exchange.getRequestHeaders();
                        String contentType = reqHeaders.getFirst("Content-Type");
                        int contentLength = Integer.parseInt(reqHeaders.getFirst("Content-Length"));
                        env.put("CONTENT_LENGTH", Integer.toString(contentLength));
                        env.put("CONTENT_TYPE", contentType != null ? contentType : "application/x-www-form-urlencoded");
                        postData = exchange.getRequestBody().readNBytes(contentLength);
                    }
                    // Get cookies
                    String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
                    if (cookieHeader != null){
                        env.put("HTTP_COOKIE", cookieHeader);
                    }
                    // Start the PHP process
                    Process p = pb.start();
                    // Write POST data into php-cgi's stdin
                    if (postData != null){
                        try (OutputStream stdin = p.getOutputStream()){
                            stdin.write(postData);
                            stdin.flush();
                        }
                    }
                    // Get the output from the proccess
                    BufferedReader output = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    // Collect the headers from the php output
                    Map<String, List<String>> phpHeaders = new LinkedHashMap<>();
                    // Build the HTML body
                    StringBuilder bodyBuilder = new StringBuilder();

                    boolean inHeaders = true;
                    String line;
                    // Read every line in the output
                    while ((line = output.readLine()) != null){
                        // Collect the headers
                        if (inHeaders) {
                            // Look for the end of the headers
                            if (line.trim().isEmpty()){
                                inHeaders = false;
                            } else {
                                // Find the index of the colon dividing key and value of the headers
                                int colonIndex = line.indexOf(":");
                                // If there is a colon
                                if (colonIndex > 0) {
                                    // Extract the key (name)
                                    String name = line.substring(0, colonIndex).trim();
                                    // Extract the value
                                    String value = line.substring(colonIndex + 1).trim();
                                    // Append the key and value to the map
                                    phpHeaders.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
                                }
                            }
                        } else {
                            // After the end of the headers append all of the lines of the body
                            bodyBuilder.append(line).append("\n");
                        }
                    }
                    output.close();
                    // Send all headers from PHP to the browser
                    for (Map.Entry<String, List<String>> header : phpHeaders.entrySet()) {
                        for (String value : header.getValue()) {
                            exchange.getResponseHeaders().add(header.getKey(), value);
                        }
                    }
                    // Check for redirecting headers
                    if (phpHeaders.containsKey("Location")){
                        //exchange.getResponseHeaders().add("Location", phpHeaders.get("Location"));
                        exchange.sendResponseHeaders(302, -1); // -1 = no body
                        return; // Break the process after the headers are being sent
                    } else {
                        // If there is no redirecting header then collect the body into the response
                        response = bodyBuilder.toString().getBytes(StandardCharsets.UTF_8);
                    }
                }
            }
            // If the host is the development url
            if (
                host.equalsIgnoreCase("dev.norlund-johan-lukas.com") || 
                host.equalsIgnoreCase("ludwig.norlund-johan-lukas.com")
                ){
                htmlFilePath = userPath;
            } else {
                // Every other case other than the develpment url
                htmlFilePath = userPath + "/static/html/" + targetFile;
            }
            // If the requested file isn't a PHP file
            if (!requestPath.endsWith(".php")){
                Path path = Paths.get(htmlFilePath);
                // Check if the  file exists
                if (!Files.exists(path) || !Files.isRegularFile(path)){
                    // Fallback if the file does not exist (404: not found :( )
                    response = Files.readAllBytes(Paths.get("www/static/html/notfound.html"));
                } else {
                    // If the file exist, load it into the response
                    response = Files.readAllBytes(Paths.get(htmlFilePath));
                }
            }
            // Set the Content-Type header for HTML
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            // Create the response
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
    ".gif", ".jpg", ".jpeg", ".JPG", ".png", ".webp", ".svg", ".bmp", ".ico", ".avif", ".heic", ".tiff"
    );
    
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Get headers
            Headers headers = exchange.getRequestHeaders();
            // Get the host
            String host = headers.getFirst("Host");
            // Get the requested path
            String requestPath = exchange.getRequestURI().getPath();
            // Block access to .php files
            if (requestPath.endsWith(".php")) {
                System.out.println("Blocked access to: " + requestPath);
                exchange.sendResponseHeaders(403, -1);
            }
            String[] requestPathSplits = requestPath.split("/");
            String fileName = requestPathSplits[requestPathSplits.length - 1];
            String contentType = "application/octet-stream";
            String user = DomainsConfig.domainMap.getOrDefault(host, null);
            String filePath = "www" + requestPath;
            // Determine the MIME type and file path
            if (requestPath.endsWith(".js")) {
                filePath = user + "/static/js/" + fileName;
                contentType = "application/javascript";
            } else if (requestPath.endsWith(".css")) {
                filePath = user + "/static/css/" + fileName;
                contentType = "text/css";
            } 
            // If the file requested is an image file
            String pathLower = requestPath.toLowerCase();
            for (String extention : IMAGE_EXTENSIONS){
                if (pathLower.endsWith(extention.toLowerCase())) {
                    filePath = user + "/static/img/" + fileName;
                    contentType = Files.probeContentType(Path.of(filePath));
                    if (contentType == null){
                        contentType = "application/octet-stream";
                    }
                    break;
                }
            }

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
                    Map<String, String> result = getUser(data.get("email"));
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
                        
                        response = handleMultipartFormData(body, requestContentType);
                    
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
                map.put("name", domainToName(map.get("domain")));
                String response = "[{\"status\": \"fail\"}]";
                boolean exists = existInTable("webserver", "users", "name", domainToName(map.get("domain")));
                if (!exists){
                    String hash = "";
                    boolean hashOk = false;
                    try {
                        hash = phpHashPass(map.get("password"));
                        hashOk = true;
                    } catch (RuntimeException e){
                        System.err.println("Error hashing password: " + e.getMessage());
                    }
                    if (hashOk) {
                        map.put("password", hash);
                        String invite = map.get("invite");
                        map.remove("invite");
                        // Insert new user 
                        insertRow("webserver", "lukas", "Tvt!77@ren", "users", map);
                        // Create the user's database
                        boolean newuser = createNewUser("lukas", "Tvt!77@ren", domainToName(map.get("domain")), map.get("password"));

                        if (newuser){
                            // Delete the invite so it can't be used again
                            boolean deleted = deleteRow("webserver", "lukas", "Tvt!77@ren", "invites", "invite", invite);
                            // Create a new directory with folders for the user
                            String userPath = "/home/lukas/users/" + domainToName(map.get("domain"));
                            createPath(userPath);
                            createPath(userPath + "/static");
                            createPath(userPath + "/static/html");
                            createPath(userPath + "/static/css");
                            createPath(userPath + "/static/js");
                            createPath(userPath + "/static/img");
                            createPath(userPath + "/static/php");
                            System.out.println("New user: " + domainToName(map.get("domain")));
                            response = "[{\"status\": \"ok\"}]";
                        }   
                    }
                }
                // Create exchange
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
    static class CreateNewTable implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if("POST".equals(exchange.getRequestMethod())){
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String request = reader.lines().collect(Collectors.joining());
                // Parse JSON into map
                Map<String, String> map = parseJsonToMap(request);
                // Extract the columns whitout the username and password
                Map<String, String> columns = new HashMap<>();
                for (Map.Entry<String, String> entry : map.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (!key.equals("username") && !key.equals("password") && !key.equals("table")){
                        columns.put(key, value);
                    }
                }
                // Create the new table
                String response = createTable(columns, map.get("table"), map.get("username") + "_db", map.get("username"), map.get("password"));
                // Make the exchange
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
    static class GameAssetsHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            Headers headers = exchange.getRequestHeaders();
            String host = headers.getFirst("Host");
            String build = "";
            if (host.equalsIgnoreCase("dev.norlund-johan-lukas.com")){ build = "NuggetsBuild";}
            else if (host.equalsIgnoreCase("ludwig.norlund-johan-lukas.com")){ build = "BSCMM";}
            String requestPath = exchange.getRequestURI().getPath();
            String filePath = requestPath.replace("game", "/home/lukas/UnityBuilds/" + build);
            System.out.println("Serving: " + filePath);
            String contentType = "application/octet-stream";
            if (requestPath.endsWith(".js")) {
                contentType = "application/javascript";
            } else if (requestPath.endsWith(".css")){
                contentType = "text/css";
            }
            // Read the file
            byte[] response = Files.readAllBytes(Paths.get(filePath));
            //
            if (filePath.endsWith(".br")){
                exchange.getResponseHeaders().set("Content-Encoding", "br");
                if (filePath.endsWith(".js.br")) contentType = "application/javascript";
                else if (filePath.endsWith(".wasm.br")) contentType = "application/wasm";
                else if (filePath.endsWith(".data.br")) contentType = "application/octet-stream";
            }
            // Set the encoding if it is a "unityweb" file and decide the content-type
            if (requestPath.endsWith(".unityweb")){
                exchange.getResponseHeaders().set("Content-Encoding", "gzip");

                if (filePath.contains(".wasm")) {
                    contentType = "application/wasm";
                } else if (filePath.contains(".js")){
                    contentType = "application/javascript";
                } else if (filePath.contains(".data")){
                    contentType = "application/octet-stream";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            // Create the response
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();

        }
    }
    static class ListAllFiles implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            if ("POST".equals(exchange.getRequestMethod())) {
                // Read request body
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String recivedString = reader.lines().collect(Collectors.joining());
                System.out.println("Recieved string: " + recivedString);
                // Parse JSON into map
                Map<String, String> map = parseJsonToMap(recivedString);
                // Get lists of the files depending on the user
                String userPath = "/home/lukas/users/" + map.get("user") + "/static/";
                System.out.println("Listing files in: " + userPath);

                StringBuilder json = new StringBuilder();
                String response = "";

                String[] types = {"html", "php", "css", "img", "js"};
                boolean success = true;
                json.append("[{");
                for(String type : types){
                    String[] files;
                    try {
                        files = filesList(userPath + type);
                        json.append("\""). append(type).append("\": [");
                        boolean first = true;
                        for(String file : files){
                            if (!first) {
                                json.append(", ");
                            }
                            json.append("\"").append(file).append("\"");
                            first = false;
                        }
                        json.append("], ");
                    } catch (RuntimeException e){
                        response = "[{\\\"status\\\": \\\"fail\\\"}]"; 
                        success = false;
                        break;
                    }
                }

                if (success) {
                    json.append("\"status\": \"ok\"").append("}]");
                    response = json.toString();
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
    static class DeleteFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            if ("POST".equals(exchange.getRequestMethod())) {
                // Get the request body
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String body = reader.lines().collect(Collectors.joining());
                // Parse the request body to Map
                System.out.println("Parsing body...");
                Map<String, String> map = parseJsonToMap(body);
                // Set the response message
                String response = "An error occured deleting the file";
                // Verify the user
                if (phpPassVerify(map.get("password"), map.get("email"))){
                    // Define the path to the file
                    Path path = Paths.get(
                        "/home/lukas/users/" + 
                        map.get("user") + 
                        "/static/" + 
                        map.get("type") +
                        "/" +
                        map.get("filename"));
                    // Make sure the file exist
                    if (Files.exists(path)) {
                        // Delete the file
                        try {
                            Files.delete(path);
                            response = "The file was deleted successfully";
                        } catch (IOException e){
                            System.out.println("Error deleting file: " + e.getMessage());
                        }
                    } else response = "The file does not exist";
                } else response = "User error";
                //Create exchange
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
    
    private static boolean phpPassVerify(String password, String email){
        System.out.println("Inside phpPassVerify...");
        try {
            Map<String, String> user = getUser(email);
            String hash = user.get("password");

            // Escape backslashes and single quotes
            password = password.replace("\\", "\\\\").replace("'", "\\'");
            hash = hash.replace("\\", "\\\\").replace("'", "\\'");

            String phpCode = String.format("echo password_verify('%s', '%s') ? 'true' : 'false';", password, hash);

            ProcessBuilder pb = new ProcessBuilder("php", "-r", phpCode);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
                String result = reader.readLine();
                System.out.println("Result: " + result);
                return "true".equals(result);
            }
        } catch (IOException e){
            throw new RuntimeException("Could not check the password", e);
        }
    }
    private static String[] filesList(String path){
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path))){
            List<String> files = new ArrayList<>();
            for (Path entry : stream){
                if (!Files.isDirectory(entry)){
                    files.add(entry.getFileName().toString());
                }
            }
            return files.toArray(new String[0]);
        } catch (IOException | DirectoryIteratorException e){
            throw new RuntimeException();
        }
    }
    private static String phpHashPass(String password){
        try {
            String escapedPassword = password.replace("'", "\\'");
            ProcessBuilder pb = new ProcessBuilder(
                "php",
                "-r",
                "echo password_hash('" + escapedPassword + "', PASSWORD_DEFAULT);"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String hashedPassword = reader.readLine();
                if (hashedPassword != null){
                    return hashedPassword;
                } else {
                    throw new RuntimeException("No output from PHP proces.");
                }
            }
        } catch (IOException e){
            throw new RuntimeException("Failed to hash password using PHP.", e);
        }
    }
    private static String createTable(Map<String, String> columns, String table, String database, String username, String password){
        String url = "jdbc:mysql://localhost:3306/" + database;
        System.out.println("Using: " + database);
        try (Connection conn = DriverManager.getConnection(url, username, password)){
            StringBuilder sql = new StringBuilder();
            sql.append("CREATE TABLE ").append(sanitizeUserName(table)).append(" (");
            sql.append("id INT NOT NULL PRIMARY KEY AUTO_INCREMENT");
            for (Map.Entry<String, String> entry : columns.entrySet()){
                String columnName = entry.getKey();
                String setting = entry.getValue();
                sql.append(", ").append(sanitizeUserName(columnName)).append(" ").append(sanitizeUserName(setting));
                if (sanitizeUserName(setting).equals("VARCHAR")) { sql.append("(255)");}
            }
            sql.append(")");
            System.out.println("Executing: " + sql.toString());
            try (Statement stmt = conn.createStatement()){
                stmt.executeUpdate(sql.toString());
                return "[{\"status\": \"ok\"}]";
            }
        } catch (Exception e){
            e.printStackTrace();
            return "[{\"status\": \"fail\"}]";
        }
    }
    private static String domainToName(String domain){
        String name = domain.replace("www.", "");
        return sanitizeUserName(name);
    }
    private static String emailToName(String email){
        String[] parts = email.split("@");
        return sanitizeUserName(parts[0]);
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
    private static Map<String, String> getUser(String email){
        String url = "jdbc:mysql://localhost:3306/webserver";
        try(Connection conn = DriverManager.getConnection(url, "lukas", "Tvt!77@ren")){
            String sql = "SELECT * FROM users WHERE email = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pstmt.setString(1, email);
            ResultSet resultSet = pstmt.executeQuery();
            Map<String, String> user = new HashMap<>();
            while (resultSet.next()){
                if (resultSet.getString("email").equals(email)) {
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
    private static boolean deleteRow(String database, String username, String password, String table, String column, String value){
        System.out.println("deleteData");
        String url = "jdbc:mysql://localhost:3306/" + database;
        String query = "DELETE FROM " + table + " WHERE " + column + " = ?"; 
        System.out.println("Query: " + query);
        try(Connection conn = DriverManager.getConnection(url, username, password)){
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, value);
            pstmt.execute();
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
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
    private static boolean existInTable(String database, String table, String column, String value){
        String url = "jdbc:mysql://localhost:3306/" + database;
        String sql = "SELECT * FROM " + table + " WHERE " + column + " = ?";        
        try (Connection conn = DriverManager.getConnection(url, "lukas", "Tvt!77@ren")){
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, value);
            System.out.println("sql: " + pstmt.toString());
            ResultSet result = pstmt.executeQuery();
            int rows = 0;
            while (result.next()) {
                rows++;
            }
            if (rows > 0) {
                return true;
            } else return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private static boolean createNewUser(String username, String password, String newuser, String newpassword){
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
                return true;
            }
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }
    private static void createPath(String path) throws IOException{
        Path newPath = Paths.get(path);
        try {
            Files.createDirectory(newPath);
            System.out.println("Directory created: " + newPath.toAbsolutePath());
        } catch (FileAlreadyExistsException e) {
            System.out.println("Directory already exsists.");
        } catch (IOException e) {
            System.out.println(e.getMessage());
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
    private static String handleMultipartFormData(byte[] body, String contentType) throws IOException {
        String boundary = contentType.split("boundary=")[1];
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        byte[] closingBoundaryBytes = ("--" + boundary + "--").getBytes(StandardCharsets.UTF_8);

        String msg = "Unknown upload error";
        
        int pos = 0;
        byte[] data = null;
        boolean allowed = true;
        String user = "temp";
        String fileName = "placeholder.jpg";
        String fileType = "img";

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
                fileName = extractFileName(headers);
                System.out.println("File name: " + fileName);
                data = fileContent;

                msg = "File uploaded successfully";
                
                if (fileName.endsWith(".html")) {
                    fileType = "html";
                } 
                else if(fileName.endsWith(".php")){
                    fileType = "php";
                } 
                else if (fileName.endsWith(".css")) {
                    fileType = "css";
                } else if(fileName.endsWith(".js")){
                    fileType = "js";
                } else {
                    boolean isImgFile = false;
                    String fileNameLower = fileName.toLowerCase();
                    for (String extention : IMAGE_EXTENSIONS){
                        if (fileNameLower.endsWith(extention.toLowerCase())) {
                            fileType = "img";
                            isImgFile = true;
                            break;
                        } 
                    }
                    if (!isImgFile) {
                        allowed = false;
                        msg = "File format not supported.";
                    }
                }
                continue;
            }

            if (headers.contains("name=\"user\"")) {
                user = new String(fileContent, StandardCharsets.UTF_8).trim();
                System.out.println("Received user: " + user);
                continue;
            }
            pos = nextBoundary;
        }
        if (data != null && allowed) {
            saveFile(fileName, data, fileType, user);
        }
        return msg;
    }
    private static void saveFile(String fileName, byte[] data, String fileType, String user) throws IOException {
        // Path uploadDir = Paths.get("uploads");
        Path uploadDir = Paths.get("/home/lukas/users", user, "static", fileType);
        
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
    private static String sanitizeUserName(String name){
        return name.replaceAll("[^a-zA-Z0-9]", "_");
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