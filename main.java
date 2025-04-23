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

public class main {

    public static void main(String[] args) throws IOException {
        int port = 80;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/static", new StaticFileHandler());
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
}