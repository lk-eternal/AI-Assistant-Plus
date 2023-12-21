package lk.eternal.ai.handller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lk.eternal.ai.util.ContentTypeUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ResourceHandler implements HttpHandler {
    private final String folder;

    public ResourceHandler(String folder) {
        this.folder = folder;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        final var inputStream = getClass().getClassLoader().getResourceAsStream(folder + requestPath);
        exchange.getResponseHeaders().set("Content-Type", ContentTypeUtil.type(requestPath));

        if (inputStream != null) {
            byte[] fileBytes = readFileBytes(inputStream);
            exchange.sendResponseHeaders(200, fileBytes.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(fileBytes);
            outputStream.close();
        } else {
            String response = "File not found";
            exchange.sendResponseHeaders(404, response.length());
            final var outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }
    }

    private byte[] readFileBytes(InputStream inputStream) throws IOException {
        try (inputStream) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }

}