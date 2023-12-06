package lk.eternal.ai;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lk.eternal.ai.dto.req.AppReq;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.model.*;
import lk.eternal.ai.plugin.*;
import lk.eternal.ai.service.ChatGPT3_5Service;
import lk.eternal.ai.service.ChatGPT4Service;
import lk.eternal.ai.service.TongYiQianWenService;
import lk.eternal.ai.util.ContentTypeUtil;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;

public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        initProperties();
        initProxy();

        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/api", new ApiHandler());
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
        final var properties = new Properties();
        LOGGER.info("Read properties file: {}", filePath);
        try (final var inputStream = Optional.ofNullable(Application.class.getClassLoader().getResourceAsStream(filePath))
                .or(() -> {
            try {
                return Optional.of(new FileInputStream(filePath));
            } catch (FileNotFoundException e) {
                return Optional.empty();
            }
        }).orElseThrow(() -> new IOException("Not found"))) {
            properties.load(inputStream);
        } catch (IOException e) {
            LOGGER.warn("Properties file can not read: {}", e.getMessage());
        }
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            System.setProperty(key, value);
        }
    }

    private static void initProxy() {
        final var proxyUrl = System.getProperty("proxy.url");
        final var proxyPort = System.getProperty("proxy.port");
        if (proxyUrl != null && proxyPort != null) {
            ProxySelector.setDefault(ProxySelector.of(new InetSocketAddress(proxyUrl, Integer.parseInt(proxyPort))));
        }
    }

    static class ApiHandler implements HttpHandler {

        public record User(String id, LinkedList<Message> messages, Model model){}

        private final Map<String, User> userMap = new HashMap<>();
        private final Map<String, Model> modelMap = new HashMap<>();

        public ApiHandler() {
            final var openaiApiUrl = System.getProperty("openai.url");
            final var openaiApiKey = System.getProperty("openai.key");

            final var tyqwApiKey = System.getProperty("tyqw.key");

            final var chatGPT35Service = new ChatGPT3_5Service(openaiApiKey, openaiApiUrl);
            final var chatGPT4Service = new ChatGPT4Service(openaiApiKey, openaiApiUrl);
            final var tyqwService = new TongYiQianWenService(tyqwApiKey);

            final var calcPlugin = new CalcPlugin();
            final var dbPlugin = new DbPlugin();
            final var httpPlugin = new HttpPlugin();
            final var googleSearchPlugin = new GoogleSearchPlugin(System.getProperty("google.key"), System.getProperty("google.search.cx"));

            PluginModel toolModel = new ToolPluginModel(chatGPT35Service);
            toolModel.addPlugin(calcPlugin);
            toolModel.addPlugin(dbPlugin);
            toolModel.addPlugin(httpPlugin);
            toolModel.addPlugin(googleSearchPlugin);
            this.modelMap.put(toolModel.getName(), toolModel);

            PluginModel cmdPluginModel = new CmdPluginModel(tyqwService);
            cmdPluginModel.addPlugin(calcPlugin);
            cmdPluginModel.addPlugin(dbPlugin);
            cmdPluginModel.addPlugin(httpPlugin);
            cmdPluginModel.addPlugin(googleSearchPlugin);
            this.modelMap.put(cmdPluginModel.getName(), cmdPluginModel);

            PluginModel formatPluginModel = new FormatPluginModel(tyqwService);
            formatPluginModel.addPlugin(calcPlugin);
            formatPluginModel.addPlugin(dbPlugin);
            formatPluginModel.addPlugin(httpPlugin);
            formatPluginModel.addPlugin(googleSearchPlugin);
            this.modelMap.put(formatPluginModel.getName(), formatPluginModel);

            NoneModel noneModel = new NoneModel(chatGPT35Service);
            this.modelMap.put(noneModel.getName(), noneModel);
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "*");

            var sessionId = this.getSessionIdFromCookie(t.getRequestHeaders());
            final var hasSessionId = sessionId != null;

            if (t.getRequestMethod().equalsIgnoreCase("post")) {
                final var req = new String(t.getRequestBody().readAllBytes());

                final var appReq = Mapper.readValueNotError(req, AppReq.class);

                if (!hasSessionId) {
                    sessionId = UUID.randomUUID().toString();
                    this.setSessionIdInCookie(t.getResponseHeaders(), sessionId);
                }
                String finalSessionId = sessionId;
                final var user = this.userMap.computeIfAbsent(sessionId, k -> new User(finalSessionId, new LinkedList<>(), this.modelMap.get(Optional.ofNullable(appReq.model())
                        .filter(this.modelMap::containsKey)
                        .orElse("none"))));
                user.messages().addLast(Message.user(req));
                final var answer = user.model.question(user.messages());

                t.sendResponseHeaders(200, answer.getBytes().length);
                OutputStream os = t.getResponseBody();
                os.write(answer.getBytes());
                os.flush();
                os.close();
            }
            if (t.getRequestMethod().equalsIgnoreCase("delete")) {
                if (hasSessionId) {
                    this.userMap.remove(sessionId);
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
}
