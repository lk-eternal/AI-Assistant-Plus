package lk.eternal.ai;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.model.Model;
import lk.eternal.ai.model.NoneModel;
import lk.eternal.ai.model.PluginModel;
import lk.eternal.ai.model.PromptModel;
import lk.eternal.ai.plugin.CalcPlugin;
import lk.eternal.ai.plugin.DbPlugin;
import lk.eternal.ai.plugin.GoogleSearchPlugin;
import lk.eternal.ai.plugin.HttpPlugin;
import lk.eternal.ai.service.ChatGPT3_5Service;
import lk.eternal.ai.util.ContentTypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;

public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        initProperties();

//        Model model = new NoneModel(new ChatGPT3_5Service(System.getProperty("openai.key")));

        PluginModel model = new PromptModel(new ChatGPT3_5Service(System.getProperty("openai.key")));
        model.addPlugin(new CalcPlugin());
        model.addPlugin(new DbPlugin());
        model.addPlugin(new GoogleSearchPlugin(System.getProperty("google.key"), System.getProperty("google.search.cx")));
        model.addPlugin(new HttpPlugin());


        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/api", new ApiHandler(model));
        server.createContext("/", new ResourceHandler("static"));
        server.setExecutor(Executors.newFixedThreadPool(1000));
        server.start();
    }

    private static void initProperties() {
        final var active = System.getenv("profiles.active");
        String filePath;
        if (active != null && !active.isBlank()) {
            LOGGER.info("profiles.active: {}", active);
            filePath = "application-" + active + ".properties";
        } else {
            LOGGER.info("profiles.active: default");
            filePath = "application.properties";
        }
        final var classLoader = Application.class.getClassLoader();
        final var properties = new Properties();
        final var resource = classLoader.getResource(filePath);
        if (resource == null) {
            LOGGER.warn("Properties file not found: {}", filePath);
            return;
        }
        LOGGER.info("Read properties file: {}", filePath);
        try (final var fileInputStream = new FileInputStream(resource.getFile())) {
            properties.load(fileInputStream);
        } catch (IOException e) {
            LOGGER.warn("Properties file can not read: {}", e.getMessage());
        }
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            System.setProperty(key, value);
        }
    }

    static class ApiHandler implements HttpHandler {


        private final Map<String, List<Message>> sessionMessageMap = new HashMap<>();
        private final Model model;

        public ApiHandler(Model model) {
            this.model = model;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "*");

            var sessionId = this.getSessionIdFromCookie(t.getRequestHeaders());
            final var hasSessionId = sessionId != null;

            if (t.getRequestMethod().equalsIgnoreCase("post")) {
                final var question = new String(t.getRequestBody().readAllBytes());

                if (!hasSessionId) {
                    sessionId = UUID.randomUUID().toString();
                    this.setSessionIdInCookie(t.getResponseHeaders(), sessionId);
                }
                final var messages = (LinkedList<Message>) this.sessionMessageMap.computeIfAbsent(sessionId, k -> new LinkedList<>());
                messages.addLast(Message.user(question));
                final var answer = this.model.question(messages);

                t.sendResponseHeaders(200, answer.getBytes().length);
                OutputStream os = t.getResponseBody();
                os.write(answer.getBytes());
                os.flush();
                os.close();
            }
            if (t.getRequestMethod().equalsIgnoreCase("delete")) {
                if (hasSessionId) {
                    this.sessionMessageMap.remove(sessionId);
                }
                t.sendResponseHeaders(200, 0);
                OutputStream os = t.getResponseBody();
                os.close();
            } else {
                t.sendResponseHeaders(200, 0);
                OutputStream os = t.getResponseBody();
                os.close();
            }
        }

        private String getSessionIdFromCookie(Headers requestHeaders) {
            return requestHeaders.getOrDefault("Cookie", Collections.emptyList())
                    .stream()
                    .flatMap(cookie -> HttpCookie.parse(cookie).stream())
                    .filter(cookie -> cookie.getName().equals("sessionId"))
                    .map(HttpCookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        private void setSessionIdInCookie(Headers responseHeaders, String sessionId) {
            HttpCookie cookie = new HttpCookie("sessionId", sessionId);
            cookie.setPath("/");
            responseHeaders.add("Set-Cookie", cookie.toString());
        }

    }

    static class ResourceHandler implements HttpHandler {
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

            final var classLoader = getClass().getClassLoader();
            final var file = Optional.ofNullable(classLoader.getResource(folder + requestPath))
                    .map(URL::getFile)
                    .map(File::new)
                    .orElse(null);

            exchange.getResponseHeaders().set("Content-Type", ContentTypeUtil.type(requestPath));

            if (file != null && file.exists() && file.isFile()) {
                byte[] fileBytes = readFileBytes(file);
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

        private byte[] readFileBytes(File file) throws IOException {
            try (InputStream inputStream = new FileInputStream(file)) {
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
}
