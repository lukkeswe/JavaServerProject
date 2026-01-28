import java.io.*;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.nio.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class Main {

    public static void main(String[] args) throws IOException {
        int port = 80;

        ExecutorService normalExecutor = Executors.newFixedThreadPool(20);
        ExecutorService videoExecutor = Executors.newFixedThreadPool(8);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/upload", new UploadHandler());
        server.createContext("/query", new QueryHandler());
        server.createContext("/deleteRows", new DeleteHandler());
        server.createContext("/invite", new CheckInvite());
        server.createContext("/newuser", new NewUser());
        server.createContext("/questionForm", new QuestionForm());
        server.createContext("/createTable", new CreateNewTable());
        server.createContext("/game", new GameAssetsHandler());
        server.createContext("/listAllFiles", new ListAllFiles());
        server.createContext("/deleteFile", new DeleteFileHandler());
        server.createContext("/getFileContent", new FetchFileContent());
        server.createContext("/saveFile", new SaveFileHandler());
        server.createContext("/getBlogContent", new FetchBlogContentHandler());
        server.createContext("/saveBlog", new SaveBlogHandler());
        server.createContext("/deleteBlog", new DeleteBlogHandler());
        server.createContext("/renameBlog", new RenameBlogHandler());
        server.createContext("/createFolder", new CreateFolderHandler());
        server.createContext("/deleteFolder", new DeleteFolderHandler());
        server.createContext("/moveIt", new MoveItHandler());
        server.createContext("/create-session", new SessionHandler());
        server.createContext("/check-session", new CheckJavaSession());
        server.createContext("/", new DelegatingHandler(normalExecutor, videoExecutor));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Server started on port " + port);
    }
    // Admin
    private static final String ADMIN_HOST = "tekknat_com";
    private static final String ADMIN_DIR = "/home/lukas/JavaServerProject/www";
    // User base directory
    private static final String USER_DIR = "/home/lukas/users/";
    // Limit video stream
    private static final AtomicInteger ACTIVE_STREAMS = new AtomicInteger(0);
    private static final int MAX_STREAMS = 8;

    private static final Set<String> TEXT_EXTENSIONS = Set.of(".html", ".php", ".css", ".js");

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
    ".gif", ".jpg", ".jpeg", ".JPG", ".png", ".webp", ".svg", ".bmp", ".ico", ".avif", ".heic", ".tiff"
    );

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(".mp4", ".mov", ".avi");

    private static final Set<String> STATIC_EXTENSIONS = Set.of(
        "/static", "/static/css", "/css", "/static/img", "/img", "/static/js", "/js"
    );

    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private static final Properties DB_PROPERTIES;
    static {
        try {
            DB_PROPERTIES = DBConfig.loadConfig();
        } catch (IOException e){
            throw new RuntimeException("Failed to load DB config", e);
        }
    }

    static class DelegatingHandler implements HttpHandler {
        private final ExecutorService normalExecutor;
        private final ExecutorService videoExecutor;

        private final RootHandler root = new RootHandler();

        DelegatingHandler(ExecutorService normalExecutor, ExecutorService videoExecutor) {
            this.normalExecutor = normalExecutor;
            this.videoExecutor = videoExecutor;
        }
        @Override
        public void handle(HttpExchange exchange) {
            exchange.setStreams(null, null);

            String path = exchange.getRequestURI().getPath();

            // Choose executor based on file extension
            boolean isVideo = path.endsWith(".mp4") || path.endsWith(".mov") || path.endsWith(".avi");

            ExecutorService exec = isVideo ? videoExecutor : normalExecutor;

            exec.execute(() -> {
                try {
                    root.handle(exchange);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
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
            // Extract the request path
            String requestPath = exchange.getRequestURI().getPath();
            String[] requestSplit = requestPath.split("/");
            
            // Define the path to the index file
            String htmlFilePath = ADMIN_DIR + "/static/index.html";
            // If no html file is specified target the index file
            String targetFile = "index.html";
            // Initial path
            String userPath = DomainsConfig.domainMap.getOrDefault(host, null);
            if (userPath == null) {
                userPath = ADMIN_DIR;
                targetFile = "notfound.html";
            }
            // Initial response
            byte[] response = null;
            // If the requested file is a html file
            if (requestPath.endsWith(".html")) {
                targetFile = requestSplit[requestSplit.length - 1];
            } else if (requestPath.endsWith(".php")){
                // Proccess the requested PHP file
                byte[] requestedPhp = runPhp(exchange);
                // If the result isn't null
                if (requestedPhp != null) {
                    response = requestedPhp;
                // Else break the exchange
                } else {
                    response = Files.readAllBytes(Paths.get("www/static/notfound.html"));
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(404, response.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response);
                    os.close();
                    exchange.close();
                    return;
                }

            } else if (requestPath.endsWith(".blog")){
                System.out.println("-- Looking for blog --");
                Path blogPath = Paths.get(userPath, "blog", requestPath.replace(".blog", ".html"));
                System.out.println("-- Path: " + blogPath.toString() + " --");
                if (Files.exists(blogPath)) {
                    System.out.println("-- Found the file! --");
                    response = Files.readAllBytes(blogPath);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, response.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response);
                    os.close();
                    exchange.close();
                    return;
                }
            }
            
            // If the host is the development url
            if (
                host.equalsIgnoreCase("dev.norlund-johan-lukas.com") || 
                host.equalsIgnoreCase("ludwig.norlund-johan-lukas.com")
                ){
                htmlFilePath = userPath;
            } else if (!requestPath.equals("/") && !requestPath.endsWith(".php")) {
                System.out.println("Iregular path: " + requestPath);
                // Check if the path points to real directory
                Path fullPath = Paths.get(userPath, "static", requestPath);
                String contentType = "text/html";
                if (!requestPath.endsWith(".html")) {
                    // If the file isn't an HTML file, check if it is a valid static file format
                    Path resolvePath = Paths.get(userPath, "static", requestPath).normalize();
                    System.out.println("Looking for: " + resolvePath.toString());
                    if (!Files.exists(resolvePath) || !Files.isRegularFile(resolvePath)) {
                        //Look for a HTML file
                        resolvePath = Paths.get(userPath, "static", requestPath, "index.html").normalize();
                        System.out.println("Looking for: " + resolvePath.toString());
                        if (!Files.exists(resolvePath) || !Files.isRegularFile(resolvePath)) {
                            // Look for a PHP file
                            resolvePath = Paths.get(userPath, "static", requestPath, "index.php").normalize();
                            System.out.println("Looking for: " + resolvePath.toString());
                            if (!Files.exists(resolvePath) || !Files.isRegularFile(resolvePath)) {
                                resolvePath = Paths.get(userPath, "static", requestPath, "index.blog").normalize();
                                System.out.println("Looking for: " + resolvePath.toString());
                                if (!Files.exists(resolvePath) || !Files.isRegularFile(resolvePath)) {
                                    System.out.println("Invalid path:" + resolvePath.toString());
                                    response = Files.readAllBytes(Paths.get("www/static/notfound.html"));
                                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                                    exchange.sendResponseHeaders(404, response.length);
                                    OutputStream os = exchange.getResponseBody();
                                    os.write(response);
                                    os.close();
                                    exchange.close();
                                    return;
                                } else {
                                    response = Files.readAllBytes(Paths.get(userPath, "blog", requestPath, "index.html"));
                                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                                    exchange.sendResponseHeaders(200, response.length);
                                    OutputStream os = exchange.getResponseBody();
                                    os.write(response);
                                    os.close();
                                    exchange.close();
                                    return;
                                }
                            } else {
                                System.out.println("Proccessing: " + resolvePath.toString());
                                // Proccess the requested PHP file
                                byte[] requestedPhp = runPhp(exchange);
                                // If the result isn't null
                                if (requestedPhp != null) {
                                    exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
                                    exchange.sendResponseHeaders(200, requestedPhp.length);
                                    OutputStream os = exchange.getResponseBody();
                                    os.write(requestedPhp);
                                    os.close();
                                    exchange.close();
                                    return;
                                // Else break the exchange
                                } else {
                                    response = Files.readAllBytes(Paths.get("www/static/notfound.html"));
                                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                                    exchange.sendResponseHeaders(404, response.length);
                                    OutputStream os = exchange.getResponseBody();
                                    os.write(response);
                                    os.close();
                                    exchange.close();
                                    return;
                                }
                            }
                    
                        }
                    }
                    // Check if it is a CSS or JavaScript file
                    if (resolvePath.toString().endsWith(".css") || resolvePath.toString().endsWith(".js") || resolvePath.toString().endsWith(".pdf")){
                        // Set the full target path to the resolvePath
                        fullPath = resolvePath;
                        // Set the content type
                        if (resolvePath.toString().endsWith(".css")) contentType = "text/css";
                        else if (resolvePath.toString().endsWith(".js")) contentType = "application/javascript";
                        else if (resolvePath.toString().endsWith(".pdf")) contentType = "application/pdf";
                    } else {
                        // Check if the file is an image or video file
                        boolean isImage = false;
                        boolean isVideo = false;
                        if (resolvePath.toString().endsWith(".mp4")) {
                            isVideo = true;
                            contentType = "video/mp4";
                        } else {
                            // Cycle through the supported file formats
                            for (String extension : IMAGE_EXTENSIONS){
                                // If the extention match a suppported format set the content type to "octet-stream"
                                if (resolvePath.toString().endsWith(extension)) {
                                    isImage = true;
                                    contentType = "application/octet-stream";
                                    if (extension.equals(".svg")) {
                                        contentType = "image/svg+xml";
                                    }
                                    break;
                                }
                            }
                        }
                        // Set the full target path to the resolve path if the file is an image or video
                        if (isImage || isVideo) fullPath = resolvePath;
                        // If the requested file isn't a supported static file, append "index.html" (fallback)
                        else fullPath = Paths.get(fullPath.toString(), targetFile);
                    }
                }
                
                if (!Files.exists(fullPath)){
                    // Serve 404 if path does not exist
                    System.out.println("invalid path: " + fullPath.toString());
                    response = Files.readAllBytes(Paths.get("www/static/html/notfound.html"));
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(404, response.length);
                } else {
                    if (fullPath.toString().endsWith(".mp4")) {
                        if (ACTIVE_STREAMS.incrementAndGet() > MAX_STREAMS) {
                            exchange.sendResponseHeaders(503, -1);
                            exchange.close();
                            return;
                        }
                        long fileLength = Files.size(fullPath);
                        String range = exchange.getRequestHeaders().getFirst("Range");

                        exchange.getResponseHeaders().set("Content-Type", contentType);
                        exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
                        exchange.getResponseHeaders().set("Connection", "close");

                        if (range == null) {
                            // No range request (send whole file)
                            exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileLength));
                            exchange.sendResponseHeaders(200, fileLength);
                            try (OutputStream os = exchange.getResponseBody()){
                                Files.copy(fullPath, os);
                            } finally {
                                ACTIVE_STREAMS.decrementAndGet();
                            }                     
                        } else { 
                            // Parse range header: e.g. "bytes=1000-"
                            String[] parts = range.replace("bytes=", "").split("-");
                            long start = Long.parseLong(parts[0]);
                            long end = (parts.length > 1 && !parts[1].isEmpty()) ? Long.parseLong(parts[1]) : fileLength - 1;
                            // Clamp range (void out of bounds)
                            start = Math.max(0, start);
                            end = Math.min(fileLength - 1, end);

                            long length = end - start + 1;

                            exchange.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                            exchange.sendResponseHeaders(206, length);

                            try (FileChannel fileChannel = FileChannel.open(fullPath, StandardOpenOption.READ);
                                OutputStream os = exchange.getResponseBody();
                                WritableByteChannel outChannel = Channels.newChannel(os)){
                                    fileChannel.position(start);
                                    long transferred = 0;
                                    while (transferred < length) {
                                        long bytes = fileChannel.transferTo(start + transferred, length - transferred, outChannel);
                                        if (bytes <= 0) break;
                                        transferred += bytes;
                                    }
                                } finally {
                                    ACTIVE_STREAMS.decrementAndGet();
                                }

                            exchange.close();
                            return;
                        }
                    } else {
                        response = Files.readAllBytes(fullPath);
                        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
                        exchange.sendResponseHeaders(200, response.length);
                    }
                }
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
                exchange.close();
                return;
            } else {
                // Every other case other than the develpment url
                htmlFilePath = userPath + "/static/" + targetFile;
            }
            // If the requested file isn't a PHP file
            if (!requestPath.endsWith(".php")){
                Path path = Paths.get(htmlFilePath);
                // Check if the  file exists
                if (!Files.exists(path) || !Files.isRegularFile(path)){
                    // Fallback if the file does not exist (404: not found :( )
                    response = Files.readAllBytes(Paths.get("www/static/notfound.html"));
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
            exchange.close();
        }
    }
    
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
                return;
            }
            String[] requestPathSplits = requestPath.split("/");
            String fileName = requestPathSplits[requestPathSplits.length - 1];
            String contentType = "application/octet-stream";
            String user = DomainsConfig.domainMap.getOrDefault(host, "www");
            String filePath = requestPath;
            // Double check if the path is valid
            Path resolvePath = Paths.get(user, filePath).normalize();
            boolean validExtension = false;
            // Compare with all the valid extensions
            for (String extension : STATIC_EXTENSIONS) {
                if (resolvePath.startsWith(Paths.get(user + extension))) {
                    validExtension = true;
                    break;
                }
            }
            // Send "403" response if the path/extension is not valid
            boolean unregularPath = false;
            if (!validExtension) {
                if (resolvePath.endsWith(".css") || resolvePath.endsWith(".js")) {
                    unregularPath = true;
                }
                for (String extension : IMAGE_EXTENSIONS){
                    if (resolvePath.endsWith(extension)) {
                        unregularPath = true;
                    }
                }
                if (!unregularPath) {
                    exchange.sendResponseHeaders(403, -1);
                    return;
                }
            }

            // Determine the MIME type and file path
            if (requestPath.endsWith(".js")) {
                if (unregularPath) filePath = resolvePath.toString();
                else filePath = user + "/static/js/" + fileName;
                contentType = "application/javascript";
            } else if (requestPath.endsWith(".css")) {
                if (unregularPath) filePath = requestPath.toString();
                else filePath = user + "/static/css/" + fileName;
                contentType = "text/css";
            } 
            // If the file requested is an image file
            String pathLower = requestPath.toLowerCase();
            for (String extention : IMAGE_EXTENSIONS){
                if (pathLower.endsWith(extention.toLowerCase())) {
                    contentType = Files.probeContentType(Path.of(requestPath));
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
                // Verify the user
                String sessionId = getJavaSessionId(exchange);
                String user = SessionManager.getUsername(sessionId);
                if (user == null) {
                    exchange.sendResponseHeaders(403, -1);
                    exchange.getResponseBody().close();
                    return;
                } else {
                    try {
                            System.out.println("Reciving upload...");
                            // Get the request headers
                            Headers requestHeaders = exchange.getRequestHeaders();
                            // Get the request content type
                            String requestContentType = requestHeaders.getFirst("Content-Type");
                            // Get the body in bytes
                            byte[] body = inputStreamToBytes(exchange.getRequestBody());
                            String response = handleMultipartFormData(body, requestContentType, user);
                            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                            exchange.sendResponseHeaders(200, responseBytes.length);
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
                String name = domainToName(map.get("domain"));
                map.put("name", name);
                String response = "[{\"status\": \"fail\"}]";
                boolean exists = existInTable("webserver", "users", "name", name);
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
                        insertRow(DB_PROPERTIES.getProperty("db.database"), DB_PROPERTIES.getProperty("db.user"), DB_PROPERTIES.getProperty("db.password"), "users", map);
                        // Create the user's database // Not for now
                        // boolean newuser = createNewUser(DB_PROPERTIES.getProperty("db.user"), DB_PROPERTIES.getProperty("db.password"), name), map.get("password")); 
                        boolean newuser = true;
                        if (newuser){
                            // Create a new directory with folders for the user
                            String userPath = USER_DIR + name;
                            createPath(userPath);
                            createPath(userPath + "/static");
                            createPath(userPath + "/static/html");
                            createPath(userPath + "/static/css");
                            createPath(userPath + "/static/js");
                            createPath(userPath + "/static/img");
                            createPath(userPath + "/static/php");
                            
                            // Create a php.ini file for the user
                            boolean ini = phpIniSetup(name);
                            // Add the domain to the domains config
                            boolean domain = DomainConfigUtil.addDomainMapping(map.get("domain"), userPath);
                            // Add the user to the php.ini config
                            boolean php = PhpConfigUtil.addPhpMapping(map.get("domain"), name);
                            if (!ini || !domain || !php) {
                                System.out.println("Error creating new user!");
                                response = "[{\"status\": \"fail\"}]";
                            } else {
                                // Delete the invite so it can't be used again
                                boolean deleted = deleteRow(DB_PROPERTIES.getProperty("db.database"), DB_PROPERTIES.getProperty("db.user"), DB_PROPERTIES.getProperty("db.password"), "invites", "invite", invite);
                                System.out.println("New user: " + name);
                                response = "[{\"status\": \"ok\"}]";
                            }
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
                // Verify the user
                String sessionId = getJavaSessionId(exchange);
                String user = SessionManager.getUsername(sessionId);
                if (user == null) {
                    exchange.sendResponseHeaders(403, -1);
                    exchange.getResponseBody().close();
                    return;
                } else {
                    // Get lists of the files depending on the user
                    String userPath = USER_DIR + user + "/static/";
                    if (user.equals(ADMIN_HOST)) userPath = ADMIN_DIR + "/static/";
                    if (!map.get("path").isEmpty() && map.get("path") != null) {
                        userPath = userPath + map.get("path");
                    }
                    System.out.println("Listing files in: " + userPath);

                    StringBuilder json = new StringBuilder();
                    String response = "";

                    String[] types = {"folder", "html", "php", "css", "img", "js", "video", "blog"};
                    boolean success = true;
                    json.append("[{");
                        String[] files;
                        try {
                            files = filesList(userPath);
                            for (String type : types){
                                json.append("\"").append(type).append("\": [");
                                boolean first = true;
                                for (String file : files){
                                    if (file.endsWith("." + type) || (file.endsWith("/") && type.equals("folder"))) {
                                        if(!first) json.append(", ");
                                        json.append("\"").append(file).append("\"");
                                        first = false;
                                    } else if (type.equals("img")) {
                                        for (String extension : IMAGE_EXTENSIONS) {
                                            if (file.toLowerCase().endsWith(extension)) {
                                                if(!first) json.append(", ");
                                                json.append("\"").append(file).append("\"");
                                                first = false;
                                                break;
                                            }
                                        }
                                    } else if (type.equals("video")) {
                                        for (String extension : VIDEO_EXTENSIONS) {
                                            if (file.toLowerCase().endsWith(extension)) {
                                                if(!first) json.append(", ");
                                                json.append("\"").append(file).append("\"");
                                                first = false;
                                                break;
                                            }
                                        }
                                    }
                                }
                                json.append("],");
                            }
                        } catch (RuntimeException e) {
                            System.err.println(e);
                            response = "[{\\\"status\\\": \\\"fail\\\"}]"; 
                            success = false;
                        }
                    if (success) {
                        json.append("\"status\": \"ok\"").append("}]");
                        response = json.toString();
                    }

                    System.out.println("Sending back: " + response.toString());

                    // Create exchange
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
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
                String sessionId = getJavaSessionId(exchange);
                String user = SessionManager.getUsername(sessionId);

                if (user == null) {
                    exchange.sendResponseHeaders(403, -1);
                    exchange.getResponseBody().close();
                    return;
                } else {
                    System.out.println("Recived path: " + map.get("path"));
                    String userPath =  USER_DIR + user + "/static/";
                    if (user.equals(ADMIN_HOST)) userPath = ADMIN_DIR + "/static/";
                    if (!map.get("path").isEmpty() && map.get("path") != null) userPath += map.get("path");
                    userPath += map.get("filename");

                    Path path = Paths.get(userPath);
                    
                    System.out.println("Delete target: " + path.toString());
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
                    
                }
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
    static class SessionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            try {
                if ("POST".equals(exchange.getRequestMethod())) {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader reader = new BufferedReader(isr);
                    String body = reader.lines().collect(Collectors.joining());

                    System.out.println("Recieved body: " + body);

                    Map<String, String> map = parseJsonToMap(body);
                    String email = map.get("email");

                    // Get the username from the database
                    StringBuilder sql = new StringBuilder();
                    sql.append("Select name FROM users WHERE email = ").append("'").append(email).append("'");
                    String result = executeQuery(
                        DB_PROPERTIES.getProperty("db.database"), 
                        DB_PROPERTIES.getProperty("db.user"), 
                        DB_PROPERTIES.getProperty("db.password"), 
                        sql.toString()
                    );
                    Map<String, String> resultMap = new HashMap<>();
                    resultMap = parseFirstJsonObject(result);

                    System.out.println("name: " + resultMap.get("name"));

                    if (email != null && !email.isEmpty()) {
                        String sessionId = SessionManager.createSession(resultMap.get("name"));
                        System.out.println("SessionId: " + sessionId);

                        String response = sessionId;
                        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                        System.out.println("Sending response...");
                        exchange.getResponseHeaders().add("Content-Type", "text/plain");
                        exchange.sendResponseHeaders(200, bytes.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(bytes);
                        os.flush();
                        os.close();
                        System.out.println("Response sent");
                    } else {
                        exchange.sendResponseHeaders(400, -1);
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } finally {
                exchange.close();
            }
        }
    }
    static class FetchFileContent implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Get the request body
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String body = reader.lines().collect(Collectors.joining());
                // Verify the user
                String sessionId = getJavaSessionId(exchange);
                String user = SessionManager.getUsername(sessionId);
                // Reject the request if verification fails
                if (user == null) {
                    exchange.sendResponseHeaders(403, -1);
                    exchange.getResponseBody().close();
                    return;
                } else {
                    // Parse the body into map
                    Map<String, String> map = parseJsonToMap(body);
                    // Initialize response
                    byte[] response;
                    // Check if file exists
                    String userPath =  USER_DIR + user + "/static/";
                    if (user.equals(ADMIN_HOST)) userPath = ADMIN_DIR + "/static/";
                    if (!map.get("path").isEmpty() && map.get("path") != null) userPath += map.get("path");
                    userPath += map.get("filename");

                    if (!isValidPath(userPath)) {
                        System.out.println(" --- INVALID PATH! --- ");
                        System.out.println(" Possible hacker: " + "\"" + user + "\"");
                        exchange.sendResponseHeaders(403, -1);
                        exchange.getResponseBody().close();
                        return;
                    }

                    Path path = Paths.get(userPath);
                    
                    if (!Files.exists(path)){
                        System.out.println("No file found at: " + path.toString());
                        exchange.sendResponseHeaders(403, -1);
                        exchange.getResponseBody().close();
                        return;
                    } else {
                        response = Files.readAllBytes(path);
                    }
                    // Create the response
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                    exchange.sendResponseHeaders(200, response.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response);
                    os.close();
                }
                
            }
        }
    }
    static class SaveFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST". equals(exchange.getRequestMethod())) {
                // Get the request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // Verify the user
                String sessionId = getJavaSessionId(exchange);
                String user = SessionManager.getUsername(sessionId);
                // Reject the request if verification fails
                if (user == null) {
                    System.out.println("Rejected: session not valid");
                    exchange.sendResponseHeaders(403, -1);
                    exchange.getResponseBody().close();
                    return;
                } else {
                    // Parse the body into map
                    Map<String, String> map = parseJsonToMap(body);
                    String content = map.get("content").replace("\r\n", "\n");
                    // Create the file's path
                    String userPath =  USER_DIR + user + "/static/";
                    if (user.equals(ADMIN_HOST)) userPath = ADMIN_DIR + "/static/";
                    if (!map.get("path").isEmpty() && map.get("path") != null) userPath += map.get("path");
                    userPath += map.get("filename");
                    // Check if the filename is valid
                    if (!isValidFilename(map.get("filename"))) {
                        System.out.println(" --- INVALID FILENAME! --- ");
                        System.out.println(" Possible hacker: " + "\"" + user + "\"");
                        exchange.sendResponseHeaders(403, -1);
                        exchange.getResponseBody().close();
                        return;
                    }
                    if (!isValidPath(userPath)) {
                        System.out.println(" --- INVALID PATH! --- ");
                        System.out.println(" Possible hacker: " + "\"" + user + "\"");
                        exchange.sendResponseHeaders(403, -1);
                        exchange.getResponseBody().close();
                        return;
                    }
                    // Save the file
                    if (writeFile(userPath, content)){
                        String msg = "Success";
                        byte[] response = msg.getBytes();
                        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                        exchange.sendResponseHeaders(200, response.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response);
                        os.close();
                    } else {
                        exchange.sendResponseHeaders(403, -1);
                        exchange.getResponseBody().close();
                        return;
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.getResponseBody().close();
                return;
            }
        }
    }
    static class CheckJavaSession implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            if ("POST".equals(exchange.getRequestMethod())){
                // Get the request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // Parse the body to map
                Map<String, String> map = parseJsonToMap(body);
                // Verify the user
                System.out.println("Checking session id: " + map.get("session"));
                String email = SessionManager.getUsername(map.get("session"));
                if (email == null) {
                    System.out.println("Session not approved!");
                    exchange.sendResponseHeaders(400, -1);
                    return;
                } else {
                    System.out.println("Session is valid!");
                    System.out.println("Sending response...");
                    byte[] response = map.get("session").getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(200, response.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response);
                    os.close();
                    System.out.println("Response sent!");
                }
                exchange.close();
                return;
            }
        }
    }
    static class CreateFolderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Get the request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // Parse the body into a map
                Map<String, String> map = parseJsonToMap(body);
                // Verify the user
                String sessionId = getJavaSessionId(exchange);
                String user = SessionManager.getUsername(sessionId);
                // Reject the request if verification fails
                if (user == null) {
                    System.out.println("Rejected: session not valid");
                    exchange.sendResponseHeaders(403, -1);
                    exchange.getResponseBody().close();
                    return;
                } else {
                    // Build the new path
                    String path = USER_DIR + user + "/static/" + map.get("path");
                    if (!isValidPath(path.toString())) {
                        System.out.println(" --- INVALID PATH! --- ");
                        System.out.println(" Possible hacker: " + "\"" + user + "\"");
                        exchange.sendResponseHeaders(403, -1);
                        exchange.getResponseBody().close();
                        return;
                    }
                    if (ADMIN_HOST.equals(user)) {
                        path = ADMIN_DIR + "/static/" + map.get("path");
                    }
                    // Create the new path
                    String msg = "Fail";
                    Path newPath = Paths.get(path);
                    try {
                        Files.createDirectory(newPath);
                        msg = "Success";
                    } catch (FileAlreadyExistsException e) {
                        System.out.println("Directory already exist: " + path);
                    }
                    byte[] response = msg.getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                    exchange.sendResponseHeaders(200, response.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response);
                    os.close();
                }
            } else {
                exchange.sendResponseHeaders(403, -1);
                exchange.getResponseBody().close();
                return;
            }
        }
    }
    static class DeleteFolderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())){
                // Get the request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // Parse the body into map
                Map<String, String> map = parseJsonToMap(body);
                // Verify the user
                String sessionId = getJavaSessionId(exchange);
                String user = SessionManager.getUsername(sessionId);
                // Reject the request if verification fails or the path is empty
                if (user == null || map.get("path").isEmpty()) {
                    if (map.get("path").isEmpty()) System.out.println("Rejected: Empty path");
                    else System.out.println("Rejected: session not valid");
                    exchange.sendResponseHeaders(403, -1);
                    exchange.getResponseBody().close();
                    return;
                } else {
                    String path = USER_DIR + user + "/static/" + map.get("path");
                    // Validate path
                    if (!isValidPath(path.toString())) {
                        System.out.println(" --- INVALID PATH! --- ");
                        System.out.println(" Possible hacker: " + "\"" + user + "\"");
                        exchange.sendResponseHeaders(403, -1);
                        exchange.getResponseBody().close();
                        return;
                    }
                    if (user.equals(ADMIN_HOST)) {
                        path = ADMIN_DIR + "/static/" + map.get("path");
                    }
                    System.out.println("Deleting: " + path.toString());
                    String msg = "Fail";
                    if (deleteFolder(Paths.get(path))){
                        msg = "Success";
                        byte[] response = msg.getBytes();
                        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                        exchange.sendResponseHeaders(200, response.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response);
                        os.close();
                    } else {
                        byte[] response = msg.getBytes();
                        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                        exchange.sendResponseHeaders(200, response.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response);
                        os.close();
                    }
                }

            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.getResponseBody().close();
                return;
            }
        }
    }
    static class MoveItHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())){
                System.out.println("moveit");
                // Get the request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // Parse the requestbody to map
                Map<String, String> map = parseJsonToMap(body);
                // Verify the user
                String sessionId = getJavaSessionId(exchange);
                String user = SessionManager.getUsername(sessionId);
                // Reject the request if verification fails or the path is empty
                if (user == null) {
                    if (map.get("path").isEmpty()) System.out.println("Rejected: Empty path");
                    else System.out.println("Rejected: session not valid");
                    exchange.sendResponseHeaders(403, -1);
                    exchange.getResponseBody().close();
                    return;
                } else {
                    // Move the file or folder
                    String msg = "Fail";
                    int responseCode = 403;
                    String userPath = USER_DIR + user + "/static/";
                    if (!isValidPath(map.get("source")) || !isValidPath(map.get("target"))) {
                        System.out.println(" --- INVALID PATH! --- ");
                        System.out.println(" Possible hacker: " + "\"" + user + "\"");
                        exchange.sendResponseHeaders(403, -1);
                        exchange.getResponseBody().close();
                        return;
                    }
                    if (user.equals(ADMIN_HOST)) {
                        userPath = ADMIN_DIR + "/static/";
                    }
                    if (moveIt(
                        Paths.get(userPath + map.get("source")), 
                        Paths.get(userPath + map.get("target"))
                        )){
                            msg = "Success";
                            responseCode = 200;
                        }
                    byte[] response = msg.getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                    exchange.sendResponseHeaders(responseCode, response.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response);
                    os.close();
                }
            } else {
                exchange.sendResponseHeaders(403, -1);
                exchange.getResponseBody().close();
                return;
            }
        }
    }
    static class SaveBlogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if("POST".equals(exchange.getRequestMethod())){
                //Gete the request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // Parse the body into Map
                Map<String, String> map = parseJsonToMap(body);
                // Verify the user
                String sessionId = getJavaSessionId(exchange);
                String user = SessionManager.getUsername(sessionId);
                // Reject the request if the verification fails
                if (user == null) {
                    exchange.sendResponseHeaders(403, -1);
                    exchange.close();
                    return;
                } else {
                    // Initial response message
                    String msg = "Fail";
                    // Save the file and the blog as a HTML file and make a '.blog' file as a symbolic file
                    String filename = map.get("filename");
                    // Validate filename
                    if (!isValidFilename(filename)){
                        System.out.println(" --- INVALID FILENAME ---");
                        System.out.println(" --- Possible hacker: \"" + user + "\"" );
                        exchange.sendResponseHeaders(403, -1);
                        exchange.close();
                        return;
                    }
                    if (!isValidPath(map.get("path"))){
                        System.out.println(" --- INVALID PATH ---");
                        System.out.println(" --- Possibe hacker: \"" + user + "\"");
                        exchange.sendResponseHeaders(403, -1);
                        exchange.close();
                        return;
                    }
                    String blogname = filename.replace(".html", ".blog");
                    int fileSize = map.get("data").length();
                    String saveData = 
                        "filename=" + filename + "\n" + 
                        "blogname=" + blogname + "\n" + 
                        "path=" + map.get("path").toString() + "\n" +
                        "size=" + fileSize;
                    if (saveFile(filename, map.get("data").getBytes(), "blog/" + map.get("path"), user)){
                        if (saveFile(blogname, saveData.getBytes(), "static/" + map.get("path"), user)) {
                            msg = "Success";
                        }
                    }
                     byte[] response = msg.getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                    exchange.sendResponseHeaders(200, response.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response);
                    os.close();
                    exchange.close();
                    return;
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
        }
    }
    static class FetchBlogContentHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())){
                // Get the requestbody
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // Parse the body into Map
                Map<String, String> map = parseJsonToMap(body);
                // Verify the user
                String sessionId = getJavaSessionId(exchange);
                String user = SessionManager.getUsername(sessionId);
                if (user == null) {
                    exchange.sendResponseHeaders(403, -1);
                    exchange.close();
                    return;
                } else {
                    System.out.println("-- Fetching blog content --");
                    Path path = Paths.get(USER_DIR, user, "blog", map.get("path").toString(), map.get("filename").toString());
                    System.out.println("-- Path: " + path.toString() + " -- ");
                    if (Files.exists(path)){
                        System.out.println("-- File exists --");
                        byte[] response = Files.readAllBytes(path);
                        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                        exchange.sendResponseHeaders(200, response.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response);
                        os.close();
                        exchange.close();
                        return;
                    } else {
                        exchange.sendResponseHeaders(403, -1);
                        exchange.close();
                        return;
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
        }
    }
    static class DeleteBlogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Validate the user
                String sessionId = getJavaSessionId(exchange);
                String user = SessionManager.getUsername(sessionId);
                if (user == null) {
                    exchange.sendResponseHeaders(403, -1);
                    exchange.close();
                    return;
                }
                // Get the request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // Parse the body into map
                Map<String, String> map = parseJsonToMap(body);
                // Make sure the needed variables are present
                if (map.get("path") == null || map.get("filename") == null){
                    exchange.sendResponseHeaders(403, -1);
                    exchange.close();
                    return;
                }
                // Build paths to the files
                Path blogFilePath = Paths.get(USER_DIR, user, "static", map.get("path"), map.get("filename"));
                Path htmlFilePath = Paths.get(USER_DIR, user, "blog", map.get("path"), map.get("filename").replace(".blog", ".html"));
                // Delete the files
                String msg = "Error deleting blog";
                boolean htmlDeleted = false;
                System.out.println("-- Deleting: " + htmlFilePath.toString() + " -- ");
                // Make sure the HTML file exsits
                if (Files.exists(htmlFilePath)){
                    System.out.println(" -- HTML file found -- ");
                    try {
                        // Delete the file
                        Files.delete(htmlFilePath);
                        htmlDeleted = true;
                        System.out.println("-- HTML file deleted -- ");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                boolean blogDeleted = false;
                System.out.println(" -- Deleting: " + blogFilePath.toString() + " -- ");
                // Make sure the BLOG file exists
                if (Files.exists(blogFilePath)){
                    System.out.println(" -- BLOG file found -- ");
                    try {
                        // Delete the file
                        Files.delete(blogFilePath);
                        blogDeleted = true;
                        System.out.println(" -- BLOG file deleted -- ");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // If successfully deleted the files change the response message
                if (blogDeleted && htmlDeleted) {
                    msg = "Success";
                }
                // Send the response
                byte[] response = msg.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
                exchange.close();
                return;
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
        }
    }
    static class RenameBlogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())){
                // Validate the user
                String sessionId = getJavaSessionId(exchange);
                String user = SessionManager.getUsername(sessionId);
                // Reject invalid user
                if (user == null) {
                    exchange.sendResponseHeaders(403, -1);
                    exchange.close();
                    return;
                }
                //Get the requestbody
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                //Parse the body into map
                Map<String, String> map = parseJsonToMap(body);
                // Validate filename
                if (!isValidFilename(map.get("filename"))){
                    System.out.println(" --- INVALID FILENAME ---");
                    System.out.println(" --- Possible hacker: \"" + user + "\"");
                    exchange.sendResponseHeaders(403, -1);
                    exchange.close();
                    return;
                }
                if (!isValidPath(map.get("path"))){
                    System.out.println(" --- INVALID PATH --- ");
                    System.out.println(" --- Possible hacker: \"" + user + "\"");
                    exchange.sendResponseHeaders(403, -1);
                    exchange.close();
                    return;
                }
                // Build the paths
                Path htmlSource = Paths.get(USER_DIR, user, "static", map.get("path"), map.get("filename"));
                Path htmlTarget = Paths.get(USER_DIR, user, "static", map.get("path"), map.get("newname"));
                Path blogSource = Paths.get(USER_DIR, user, "blog", map.get("path"), map.get("filename").replace(".blog", ".html"));
                Path blogTarget = Paths.get(USER_DIR, user, "blog", map.get("path"), map.get("newname").replace(".blog", ".html"));
                //Move the paths
                String msg = "fail";
                if (moveIt(htmlSource, htmlTarget) && moveIt(blogSource, blogTarget)) msg = "Success";
                byte[] response = msg.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
                exchange.close();
                return;
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
        }
    }
    
    // Helper methods
    private static boolean moveIt(Path source, Path target){
        // Check if the path exists
        if (!Files.exists(source)) return false;
        // Move the file or path
        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private static boolean deleteFolder(Path path) throws IOException {
        // Check if the path exsists
        if (!Files.exists(path)) return false;
        // Walk through all files and delete them
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
                // Walk through all the files
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        Files.delete(file); // Delete file

                    } catch (IOException e) {
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                // Walk through all the directories
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) {
                    if (e != null) return FileVisitResult.TERMINATE;
                    try {
                        Files.delete(dir); // Delete directory
                    } catch (IOException ex) {
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            return false;
        }
        
        return !Files.exists(path); // Double-check that it’s gone
    }
    private static byte[] runPhp(HttpExchange exchange){
        byte[] body;

        // Get the headers
        Headers headers = exchange.getRequestHeaders();
        // Get the host
        String host = headers.getFirst("Host");
        // If there is no host
        if (host == null) host = "unknown";
        // Extract the request path
        String requestPath = exchange.getRequestURI().getPath();
        // Initial path
        String userPath = DomainsConfig.domainMap.getOrDefault(host, null);

        Path resolvePath = Paths.get(userPath, "static", requestPath);
        Path indexPath = Paths.get(resolvePath.toString(), "index.php");
        try {
            // Check if the file exist
            if (
                (!Files.exists(resolvePath) || !Files.isRegularFile(resolvePath)) &&
                !Files.exists(indexPath)
            ) {
                body = Files.readAllBytes(Paths.get("www/static/notfound.html"));
            } else {
                String phpFilePath = resolvePath.toString();
                if (!Files.exists(resolvePath) || !Files.isRegularFile(resolvePath)) {
                    phpFilePath = indexPath.toString();
                }
                // Process the file with PHP if it is a PHP file
                String username = PhpConfig.phpMap.getOrDefault(host, null);
                String phpIniPath;
                boolean tekknat = false;
                if (host.equals("tekknat.com")){
                    phpIniPath = "www/static/tekknat.ini"; 
                    tekknat = true;
                } else phpIniPath = "/etc/php/users/" + username + ".ini";
                // Create the ProcessBuilder with a "-c" flag
                ProcessBuilder pb = new ProcessBuilder("php-cgi", "-c", phpIniPath);
                // Set working directory
                if (tekknat) {
                    pb.directory(new File("www/static/"));
                } else pb.directory(new File(userPath + "/static/"));
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
                    return null; // Break the proccess
                } else {
                    // If there is no redirecting header then collect the body into the response
                    body = bodyBuilder.toString().getBytes(StandardCharsets.UTF_8);
                }
            }

            return body;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        
    }
    private static boolean writeFile(String filePath, String content) {
        Path path = Paths.get(filePath);
        try {
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Saved: " + path.toString());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private static String getJavaSessionId(HttpExchange exchange) {
        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");
        if (cookieHeaders != null) {
            for (String header : cookieHeaders) {
                System.out.println(header);
                for (String cookie : header.split(";")) {
                    String[] parts = cookie.trim().split("=");
                    if (parts.length == 2 && parts[0].equals("javasession")) {
                        return parts[1];
                    }
                }
            }
        }
        return null;
    }
    private static Map<String, String> parseFormData(String body){
        Map<String, String> result = new HashMap<>();
        for(String pair: body.split("&")){
            String[] parts = pair.split("=");
            if (parts.length == 2) {
                String key      = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value    = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                result.put(key, value); 
            }
        }
        return result;
    }
    private static boolean phpIniSetup(String user){
        String iniPath = "/home/lukas/php_ini/";
        try {
            // Ensure directory exist
            File iniDir = new File(iniPath);
            if (!iniDir.exists()) {
                iniDir.mkdirs();
            }
            // Target ini file
            File iniFile = new File(iniPath + user + ".ini");
            if (iniFile.exists()) {
                System.out.println("\".ini\" file already exists for user: " + user);
                return false;
            }

            // User's web root path for open_basedir
            String userPath = USER_DIR + user + "/static/";

            // Build contents
            StringBuilder iniContent = new StringBuilder();
            iniContent.append("open_basedir=").append(userPath).append("\n");
            iniContent.append("disable_functions=exec,passthru,shell_exec,system,proc_open,popen,pcntl_exec\n");
            iniContent.append("allow_url_fopen=Off\n");
            iniContent.append("allow_url_include=Off\n");
            iniContent.append("max_execution_time=5\n");
            iniContent.append("memory_limit=32M\n");
            iniContent.append("upload_max_filesize=2M\n");
            iniContent.append("post_max_size=4M\n");
            iniContent.append("display_errors=Off\n");

            // Copy default config first
            Path defaultPhpIni = Paths.get("/etc/php/8.1/cgi/php.ini");
            Files.copy(defaultPhpIni, iniFile.toPath());

            // Write the ini content
            Files.writeString(
                iniFile.toPath(), 
                "\n; --Per user restrictions--\n" + iniContent.toString(), 
                StandardCharsets.UTF_8,
                StandardOpenOption.APPEND
                );
            System.out.println("php.ini created for user: " + user);
            return true;
        } catch (IOException e) {
            System.out.println("Error creating php.ini for user: " + user + " - " + e.getMessage());
            return false;
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
                if (Files.isDirectory(entry)){
                    files.add(entry.getFileName().toString() + "/");
                } else {
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
        try (Connection conn = DriverManager.getConnection(DB_PROPERTIES.getProperty("db.url"), DB_PROPERTIES.getProperty("db.user"), DB_PROPERTIES.getProperty("db.password"))){
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
    private static Map<String, String> parseJsonToMap(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.trim();

        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1); // remove { }
        }

        int i = 0;
        while (i < json.length()) {
            // Skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;

            // --- Parse key ---
            if (json.charAt(i) != '"') throw new IllegalArgumentException("Expected '\"' at key start");
            int keyStart = ++i;
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') i++; // skip escaped char
                i++;
            }
            String key = unescapeJson(json.substring(keyStart, i));
            i++; // skip closing quote

            // Skip whitespace and colon
            while (i < json.length() && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ':')) i++;

            // --- Parse value ---
            if (json.charAt(i) != '"') throw new IllegalArgumentException("Expected '\"' at value start");
            int valStart = ++i;
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') i++; // skip escaped char
                i++;
            }
            String value = unescapeJson(json.substring(valStart, i));
            i++; // skip closing quote

            map.put(key, value);

            // Skip whitespace and optional comma
            while (i < json.length() && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) i++;
        }

        return map;
    }

    // --- Helper: decode JSON escape sequences ---
    private static String unescapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        break;
                    default: sb.append(next); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    // --- Helper: Parse first object in a JSON array ---
    private static Map<String, String> parseFirstJsonObject(String json){
        json = json.trim();
        if (json.startsWith("[")) {
            // Find first '{' and it's matching '}'
            int start = json.indexOf('{');
            int end = json.indexOf('}', start);
            if (start != -1 && end != -1) {
                String object = json.substring(start, end + 1);
                return parseJsonToMap(object);
            }
            // If it's already a single object
            if (json.startsWith("{")){
                return parseJsonToMap(json);
            }
        }
        return new HashMap<>();
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
        try(Connection conn = DriverManager.getConnection(DB_PROPERTIES.getProperty("db.url"), DB_PROPERTIES.getProperty("db.user"), DB_PROPERTIES.getProperty("db.password"))){
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
        try (Connection conn = DriverManager.getConnection(DB_PROPERTIES.getProperty("db.url"), DB_PROPERTIES.getProperty("db.user"), DB_PROPERTIES.getProperty("db.password"))){
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
        try (Connection conn = DriverManager.getConnection(url, DB_PROPERTIES.getProperty("db.user"), DB_PROPERTIES.getProperty("db.password"))){
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
    
    private static String handleMultipartFormData(byte[] body, String contentType, String user) throws IOException {
        String boundary = contentType.split("boundary=")[1];
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        byte[] closingBoundaryBytes = ("--" + boundary + "--").getBytes(StandardCharsets.UTF_8);

        String msg = "アップロードのエラー！";
        
        int pos = 0;
        byte[] data = null;
        String path = "";

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
                pos = nextBoundary;  // IMPORTANT: Still advance pos
                continue;
            }

            String headers = new String(part, 0, headerEnd, StandardCharsets.UTF_8);
            byte[] fileContent = Arrays.copyOfRange(part, headerEnd + 4, part.length);


            if (headers.contains("filename=\"")) {
                String relativePath = extractFilePath(headers);
                String fullPath = USER_DIR + user + "/static/" + path + relativePath;

                if (user.equals(ADMIN_HOST)) {
                    fullPath = ADMIN_DIR + "/static/" + path + relativePath;
                }
                
                System.out.println("Full path: " + fullPath);
                System.out.println("File path: " + relativePath);
                if (relativePath != null && !relativePath.isEmpty()){
                    data = fileContent;
                    // Create target file with directory structure
                    File targetFile = new File(fullPath);
                    System.out.println("folderPath: " + fullPath);
                    targetFile.getParentFile().mkdirs(); // Ensure dirs exist

                    try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                        fos.write(data);
                    }
                    msg = "アップロードが成功しました。";
                }
                // REMOVED continue here
            }

            if (headers.contains("name=\"path\"")){
                path = new String(fileContent, StandardCharsets.UTF_8).trim();
                System.out.println("Recived path: " + path);
            }
            
            // MOVED pos update here - now it happens for EVERY part
            pos = nextBoundary;
        }
        return msg;
    }
    private static boolean saveFile(String fileName, byte[] data, String path, String user) {
        try {
            Path uploadDir = Paths.get("/home/lukas/users", user, path);

            if (user.equals(ADMIN_HOST)) {
                uploadDir = Paths.get(ADMIN_DIR, path);
            }
            
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path filePath = uploadDir.resolve(sanitizeFileName(fileName));
            Files.write(filePath, data);
            System.out.println("Saved file: " + filePath);
            return true;
        } catch (IOException e){
            return false;
        }
        
    }
    private static String extractFilePath(String headers){
        int start = headers.indexOf("filename=\"");
        if (start == -1) return null;
        start += 10;
        int end = headers.indexOf("\"", start);
        if (end == -1) return null;
        String fullPath = headers.substring(start, end);
        // Normalize path separators (Windows vs Unix)
        return fullPath.replace("\\", "/");
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
    private static boolean isValidFilename(String filename) {
        if (filename.isEmpty()) return false;
        if (filename.equals(".") || filename.equals("..")) return false;
        if (filename.startsWith(".")) return false;
        if (filename.length() > 255) return false;
        return VALID_PATTERN.matcher(filename).matches();
    }
    private static boolean isValidPath(String path){
        if (path.isEmpty()) return false;
        String[] parts = path.split("/");
        for (String part:  parts){
            if (part.isEmpty()) return false;
            if (!isValidFilename(part)) return false;
        }
        return true;
    }
}