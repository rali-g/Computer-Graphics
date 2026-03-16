package server;

import algorithm.CrossPlot;
import model.Point2D;
import model.ControlPolygon;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CrossPlotServer {

    private static final int PORT = 8080;
    private HttpServer server;
    private CrossPlot crossPlot;

    public CrossPlotServer() {
        this.crossPlot = new CrossPlot();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", new StaticFileHandler());

        server.createContext("/api/curve", new CurveHandler());

        server.createContext("/api/tension", new TensionHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Cross Plot Server стартиран на http://localhost:" + PORT);
        System.out.println("Ctrl+C за спиране");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Сървърът е спрян.");
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            String basePath = System.getProperty("user.dir");
            Path filePath = Paths.get(basePath, "web", path.substring(1));

            if (Files.exists(filePath)) {
                byte[] content = Files.readAllBytes(filePath);
                String contentType = getContentType(path);

                exchange.getResponseHeaders().add("Content-Type", contentType);
                exchange.sendResponseHeaders(200, content.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            } else {
                String response = "404 - File Not Found: " + path;
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css; charset=UTF-8";
            if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (path.endsWith(".json")) return "application/json; charset=UTF-8";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".svg")) return "image/svg+xml";
            return "text/plain; charset=UTF-8";
        }
    }

    private class CurveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (exchange.getRequestMethod().equals("POST")) {
                InputStream is = exchange.getRequestBody();
                String requestBody = new String(is.readAllBytes());

                try {
                    ControlPolygon polygon = ControlPolygon.fromJson(requestBody);
                    List<Point2D> points = polygon.getPoints();

                    String response = crossPlot.generateFullResponse(points);

                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    byte[] responseBytes = response.getBytes("UTF-8");
                    exchange.sendResponseHeaders(200, responseBytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                } catch (Exception e) {
                    String error = "{\"error\":\"" + e.getMessage() + "\"}";
                    exchange.sendResponseHeaders(400, error.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(error.getBytes());
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, 0);
                exchange.getResponseBody().close();
            }
        }
    }

    private class TensionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (exchange.getRequestMethod().equals("POST")) {
                InputStream is = exchange.getRequestBody();
                String requestBody = new String(is.readAllBytes());

                try {
                    double tension = Double.parseDouble(requestBody.replaceAll("[^0-9.-]", ""));
                    crossPlot.setTension(tension);

                    String response = "{\"tension\":" + crossPlot.getTension() + "}";
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length());

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } catch (Exception e) {
                    String error = "{\"error\":\"Invalid tension value\"}";
                    exchange.sendResponseHeaders(400, error.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(error.getBytes());
                    }
                }
            } else if (exchange.getRequestMethod().equals("GET")) {
                String response = "{\"tension\":" + crossPlot.getTension() + "}";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, 0);
                exchange.getResponseBody().close();
            }
        }
    }

    public static void main(String[] args) {
        CrossPlotServer server = new CrossPlotServer();
        try {
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stop();
            }));

        } catch (IOException e) {
            System.err.println("Грешка при стартиране на сървъра: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
