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
}