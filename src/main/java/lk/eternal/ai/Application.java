package lk.eternal.ai;

import cn.hutool.http.HttpStatus;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lk.eternal.ai.dto.req.AppReq;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.model.*;
import lk.eternal.ai.plugin.CalcPlugin;
import lk.eternal.ai.plugin.DbPlugin;
import lk.eternal.ai.plugin.GoogleSearchPlugin;
import lk.eternal.ai.plugin.HttpPlugin;
import lk.eternal.ai.service.AiModel;
import lk.eternal.ai.service.ChatGPT3_5AiModel;
import lk.eternal.ai.service.ChatGPT4AiModel;
import lk.eternal.ai.service.TongYiQianWenAiModel;
import lk.eternal.ai.util.ContentTypeUtil;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.util.*;
import java.util.concurrent.Executors;

public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws IOException {
        initProperties(args);
        initProxy();

        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/api", new ApiHandler());
        server.createContext("/", new ResourceHandler("static"));
        server.setExecutor(Executors.newFixedThreadPool(1000));
        server.start();
    }

    private static void initProperties(String[] args) {
        //项目内默认配置文件
        try (final var inputStream = Application.class.getClassLoader().getResourceAsStream("application.properties")) {
            final var properties = new Properties();
            properties.load(inputStream);
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                System.setProperty(key, value);
            }
        } catch (IOException e) {
            LOGGER.warn("Read inner application.properties error:{}", e.getMessage());
        }

        //项目外默认配置文件
        try (final var inputStream = new FileInputStream("application.properties")) {
            final var properties = new Properties();
            properties.load(inputStream);
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                System.setProperty(key, value);
            }
        } catch (IOException e) {
            LOGGER.warn("Read outer application.properties error:{}", e.getMessage());
        }

        final var active = Arrays.stream(args)
                .filter(a -> a.startsWith("profiles.active="))
                .findFirst()
                .map(a -> a.split("=")[1])
                .orElseGet(() -> System.getenv("profiles.active"));
        if (active != null && !active.isBlank()) {
            LOGGER.info("profiles.active: {}", active);
            final var filePath = "application-" + active + ".properties";
            LOGGER.info("Read properties file: {}", filePath);
            //项目内指定配置文件
            try (final var inputStream = Application.class.getClassLoader().getResourceAsStream(filePath)) {
                final var properties = new Properties();
                properties.load(inputStream);
                for (String key : properties.stringPropertyNames()) {
                    String value = properties.getProperty(key);
                    System.setProperty(key, value);
                }
            } catch (IOException e) {
                LOGGER.warn("Read inner {} error:{}", filePath, e.getMessage());
            }

            //项目外指定配置文件
            try (final var inputStream = new FileInputStream(filePath)) {
                final var properties = new Properties();
                properties.load(inputStream);
                for (String key : properties.stringPropertyNames()) {
                    String value = properties.getProperty(key);
                    System.setProperty(key, value);
                }
            } catch (IOException e) {
                LOGGER.warn("Read outer {} error:{}", filePath, e.getMessage());
            }
        }

        //启动参数
        for (String arg : args) {
            final var split = arg.split("=");
            if (split.length == 2) {
                System.setProperty(split[0], split[1]);
            }
        }
    }

    private static void initProxy() {
        final var proxyUrl = System.getProperty("proxy.url");
        final var proxyPort = System.getProperty("proxy.port");
        if (proxyUrl != null && !proxyUrl.isBlank() && proxyPort != null && !proxyPort.isBlank()) {
            ProxySelector.setDefault(ProxySelector.of(new InetSocketAddress(proxyUrl, Integer.parseInt(proxyPort))));
        }
    }

    static class ApiHandler implements HttpHandler {

        public record User(String id, LinkedList<Message> messages) {
        }

        private final Map<String, User> userMap = new HashMap<>();
        private final Map<String, ToolModel> toolModelMap = new HashMap<>();
        private final Map<String, AiModel> aiModelMap = new HashMap<>();

        public ApiHandler() {
            final var openaiApiUrl = System.getProperty("openai.url");
            final var openaiApiKey = System.getProperty("openai.key");
            final var tyqwApiKey = System.getProperty("tyqw.key");
            final var chatGPT35Service = new ChatGPT3_5AiModel(openaiApiKey, openaiApiUrl);
            final var chatGPT4Service = new ChatGPT4AiModel(openaiApiKey, openaiApiUrl);
            final var tyqwService = new TongYiQianWenAiModel(tyqwApiKey);
            this.aiModelMap.put(chatGPT35Service.getName(), chatGPT35Service);
            this.aiModelMap.put(chatGPT4Service.getName(), chatGPT4Service);
            this.aiModelMap.put(tyqwService.getName(), tyqwService);

            final var calcPlugin = new CalcPlugin();
            final var dbPlugin = new DbPlugin();
            final var httpPlugin = new HttpPlugin();
            final var googleSearchPlugin = new GoogleSearchPlugin(System.getProperty("google.key"), System.getProperty("google.search.cx"));

            BaseToolModel toolModel = new NativeToolModel();
            toolModel.addPlugin(calcPlugin);
            toolModel.addPlugin(dbPlugin);
            toolModel.addPlugin(httpPlugin);
            toolModel.addPlugin(googleSearchPlugin);
            this.toolModelMap.put(toolModel.getName(), toolModel);

            BaseToolModel cmdPluginModel = new CmdToolModel();
            cmdPluginModel.addPlugin(calcPlugin);
            cmdPluginModel.addPlugin(dbPlugin);
            cmdPluginModel.addPlugin(httpPlugin);
            cmdPluginModel.addPlugin(googleSearchPlugin);
            this.toolModelMap.put(cmdPluginModel.getName(), cmdPluginModel);

            BaseToolModel formatPluginModel = new FormatToolModel();
            formatPluginModel.addPlugin(calcPlugin);
            formatPluginModel.addPlugin(dbPlugin);
            formatPluginModel.addPlugin(httpPlugin);
            formatPluginModel.addPlugin(googleSearchPlugin);
            this.toolModelMap.put(formatPluginModel.getName(), formatPluginModel);

            NoneToolModel noneModel = new NoneToolModel();
            this.toolModelMap.put(noneModel.getName(), noneModel);
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
                if (appReq == null) {
                    response(t, "无效请求", HttpStatus.HTTP_BAD_REQUEST);
                    return;
                }

                if (!hasSessionId) {
                    sessionId = UUID.randomUUID().toString();
                    this.setSessionIdInCookie(t.getResponseHeaders(), sessionId);
                }
                String finalSessionId = sessionId;
                final var user = this.userMap.computeIfAbsent(sessionId, k -> new User(finalSessionId, new LinkedList<>()));

                final var aiModelName = Optional.ofNullable(appReq.aiModel())
                        .filter(this.aiModelMap::containsKey)
                        .orElse("tyqw");
                final var toolModelName = Optional.ofNullable(appReq.toolModel())
                        .filter(this.toolModelMap::containsKey)
                        .orElse("none");
                if (aiModelName.equals("gpt4") && !"lk123".equals(appReq.gpt4Code())) {
                    response(t, "邀请码不正确", HttpStatus.HTTP_UNAUTHORIZED);
                    return;
                }
                if (aiModelName.equals("tyqw") && toolModelName.equals("native")) {
                    response(t, "通义千问不支持官方原生工具", HttpStatus.HTTP_BAD_REQUEST);
                    return;
                }

                user.messages().addLast(Message.user(req));
                final var answer = this.toolModelMap.get(toolModelName).question(this.aiModelMap.get(aiModelName), user.messages());

                t.sendResponseHeaders(200, answer.getBytes().length);
                OutputStream os = t.getResponseBody();
                os.write(answer.getBytes());
                os.close();
            } else if (t.getRequestMethod().equalsIgnoreCase("delete")) {
                if (hasSessionId) {
                    this.userMap.remove(sessionId);
                }
                response(t, "", HttpStatus.HTTP_OK);
            } else {
                response(t, "", HttpStatus.HTTP_OK);
            }
        }

        private static void response(HttpExchange t, String message, int rCode) throws IOException {
            t.sendResponseHeaders(rCode, message.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(message.getBytes());
            os.close();
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
