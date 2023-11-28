package lk.eternal.ai;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lk.eternal.ai.model.Model;
import lk.eternal.ai.model.PromptModel;
import lk.eternal.ai.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;

public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        initProperties();

        Model model = new PromptModel(new ChatGPT3_5Service(System.getProperty("openai.key")));
        model.addService(new CalcService());
        model.addService(new SqlService());
        model.addService(new HttpService());

        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        server.setExecutor(Executors.newFixedThreadPool(1000));
        server.createContext("/api", t -> {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "*");

            if (t.getRequestMethod().equalsIgnoreCase("post")) {
                final var sessionId = getSessionIdFromCookie(t.getRequestHeaders());
                final var body = new String(t.getRequestBody().readAllBytes());
                final var answer = model.question(sessionId, body);
                setSessionIdInCookie(t.getResponseHeaders(), sessionId);
                t.sendResponseHeaders(200, answer.getBytes().length);
                OutputStream os = t.getResponseBody();
                os.write(answer.getBytes());
                os.flush();
                os.close();
            } else {
                t.sendResponseHeaders(200, 0);
                OutputStream os = t.getResponseBody();
                os.close();
            }
        });

        server.createContext("/", new ResourceHandler("LK.html"));

        server.setExecutor(null);
        server.start();
    }

    private static void initProperties() {
        final var active = System.getenv("profiles.active");
        String filePath;
        if(active != null && !active.isBlank()){
            LOGGER.info("profiles.active: {}", active);
            filePath = "application-" + active + ".properties";
        }else{
            filePath = "application.properties";
        }
        ClassLoader classLoader = Application.class.getClassLoader();
        final var properties = new Properties();
        final var resource = classLoader.getResource(filePath);
        if (resource == null) {
            LOGGER.warn("Not found application.properties");
            return;
        }
        try (FileInputStream fileInputStream = new FileInputStream(resource.getFile())) {
            properties.load(fileInputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 遍历所有属性并将其添加到系统属性中
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            System.setProperty(key, value);
        }
    }

    private static String getSessionIdFromCookie(Headers requestHeaders) {
        List<String> cookies = requestHeaders.get("Cookie");
        if (cookies != null) {
            for (String cookie : cookies) {
                List<HttpCookie> httpCookies = HttpCookie.parse(cookie);
                for (HttpCookie httpCookie : httpCookies) {
                    if (httpCookie.getName().equals("sessionId")) {
                        return httpCookie.getValue();
                    }
                }
            }
        }
        return generateSessionId();
    }

    private static void setSessionIdInCookie(Headers responseHeaders, String sessionId) {
        HttpCookie cookie = new HttpCookie("sessionId", sessionId);
        cookie.setPath("/");
        responseHeaders.add("Set-Cookie", cookie.toString());
    }

    private static String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    static class ResourceHandler implements HttpHandler {
        private String htmlFileName;

        public ResourceHandler(String htmlFileName) {
            this.htmlFileName = htmlFileName;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.equals("/")) {
                requestPath = htmlFileName;
            }

            ClassLoader classLoader = getClass().getClassLoader();
            String filePath = classLoader.getResource(requestPath).getFile();
            File file = new File(filePath);

            if (file.exists() && file.isFile()) {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, fileContent.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(fileContent);
                outputStream.close();
            } else {
                String response = "File not found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            }
        }
    }
}
