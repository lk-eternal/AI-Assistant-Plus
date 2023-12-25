package lk.eternal.ai.handller;

import cn.hutool.http.HttpStatus;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lk.eternal.ai.dto.req.AppReq;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.model.ai.AiModel;
import lk.eternal.ai.model.ai.ChatGPTAiModel;
import lk.eternal.ai.model.ai.GeminiAiModel;
import lk.eternal.ai.model.ai.TongYiQianWenAiModel;
import lk.eternal.ai.model.tool.*;
import lk.eternal.ai.plugin.CalcPlugin;
import lk.eternal.ai.plugin.GoogleSearchPlugin;
import lk.eternal.ai.plugin.HttpPlugin;
import lk.eternal.ai.plugin.Plugin;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class ApiHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiHandler.class);

    private final String[] allowedOrigins;

    private final Map<String, LinkedList<Message>> userMessageMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ScheduledFuture<?>> autoRemoveUserMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private final Map<String, ToolModel> toolModelMap = new HashMap<>();
    private final Map<String, AiModel> aiModelMap = new HashMap<>();

    public ApiHandler() {
        allowedOrigins = Optional.ofNullable(System.getProperty("allowed_origins"))
                .filter(Predicate.not(String::isBlank))
                .map(origins -> origins.split(","))
                .orElse(null);
        final var openaiApiUrl = System.getProperty("openai.url");
        final var openaiApiKey = System.getProperty("openai.key");
        final var tyqwApiKey = System.getProperty("tyqw.key");
        final var chatGPT35Service = new ChatGPTAiModel(openaiApiKey, openaiApiUrl, "gpt3.5", "gpt-3.5-turbo-1106");
        final var chatGPT4Service = new ChatGPTAiModel(openaiApiKey, openaiApiUrl, "gpt4", "gpt-4-1106-preview");
        final var tyqwService = new TongYiQianWenAiModel(tyqwApiKey);
        final var geminiService = new GeminiAiModel(System.getProperty("google.ai.url"), System.getProperty("google.ai.key"));
        this.aiModelMap.put(chatGPT35Service.getName(), chatGPT35Service);
        this.aiModelMap.put(chatGPT4Service.getName(), chatGPT4Service);
        this.aiModelMap.put(tyqwService.getName(), tyqwService);
        this.aiModelMap.put(geminiService.getName(), geminiService);

        final var plugins = new ArrayList<Plugin>();
        plugins.add(new CalcPlugin());
        plugins.add(new HttpPlugin());
        plugins.add(new GoogleSearchPlugin(System.getProperty("google.key"), System.getProperty("google.search.cx")));
//        plugins.add(new DbPlugin());
//        plugins.add(new SshPlugin(System.getProperty("ssh.username"), System.getProperty("ssh.password"), System.getProperty("ssh.host"), Integer.parseInt(System.getProperty("ssh.port"))));
//        plugins.add(new CmdPlugin());

        BaseToolModel toolModel = new NativeToolModel();
        plugins.forEach(toolModel::addPlugin);
        this.toolModelMap.put(toolModel.getName(), toolModel);

        BaseToolModel cmdPluginModel = new CmdToolModel();
        plugins.forEach(cmdPluginModel::addPlugin);
        this.toolModelMap.put(cmdPluginModel.getName(), cmdPluginModel);

        BaseToolModel formatPluginModel = new FormatToolModel();
        plugins.forEach(formatPluginModel::addPlugin);
        this.toolModelMap.put(formatPluginModel.getName(), formatPluginModel);

        NoneToolModel noneModel = new NoneToolModel();
        this.toolModelMap.put(noneModel.getName(), noneModel);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!validOrigin(exchange)) {
            return;
        }

        if (exchange.getRequestMethod().equalsIgnoreCase("post")) {
            final var req = new String(exchange.getRequestBody().readAllBytes());
            LOGGER.info(req);
            final var appReq = Mapper.readValueNotError(req, AppReq.class);
            if (appReq == null) {
                response(exchange, "无效请求", HttpStatus.HTTP_BAD_REQUEST);
                return;
            }

            var sessionId = this.getSessionIdFromCookie(exchange.getRequestHeaders());
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
                this.setSessionIdInCookie(exchange.getResponseHeaders(), sessionId);
            }
            resetUserLifeTime(sessionId);
            final var messages = this.userMessageMap.computeIfAbsent(sessionId, k -> new LinkedList<>());

            final var aiModelName = Optional.ofNullable(appReq.aiModel())
                    .filter(this.aiModelMap::containsKey)
                    .orElse("tyqw");
            final var toolModelName = Optional.ofNullable(appReq.toolModel())
                    .filter(this.toolModelMap::containsKey)
                    .orElse("none");
            if (aiModelName.equals("gpt4") && !"lk123".equals(appReq.gpt4Code())) {
                response(exchange, "邀请码不正确", HttpStatus.HTTP_UNAUTHORIZED);
                return;
            }
            if (toolModelName.equals("native") && !aiModelName.equals("gpt3.5") && !aiModelName.equals("gpt4")) {
                response(exchange, "通义千问不支持官方原生工具", HttpStatus.HTTP_BAD_REQUEST);
                return;
            }

            messages.addLast(Message.user(appReq.question()));

            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0);
            final var os = exchange.getResponseBody();
            this.toolModelMap.get(toolModelName).question(this.aiModelMap.get(aiModelName), messages, resp -> {
                try {
                    final var respStr = Mapper.writeAsStringNotError(resp);
                    if (respStr != null) {
                        os.write(respStr.getBytes());
                        os.write("[PACKAGE_END]".getBytes());
                        os.flush();
                    }
                } catch (IOException e) {
                    LOGGER.error("write resp error: {}", e.getMessage(), e);
                }
            });
            os.close();
        } else if (exchange.getRequestMethod().equalsIgnoreCase("delete")) {
            var sessionId = this.getSessionIdFromCookie(exchange.getRequestHeaders());
            if (sessionId != null) {
                removeUser(sessionId);
            }
            response(exchange, "", HttpStatus.HTTP_OK);
        } else if (exchange.getRequestMethod().equalsIgnoreCase("options")) {
            response(exchange, "", HttpStatus.HTTP_OK);
        } else {
            response(exchange, "", HttpStatus.HTTP_NOT_FOUND);
        }
    }

    private boolean validOrigin(HttpExchange exchange) throws IOException {
        if (allowedOrigins != null) {
            String origin = exchange.getRequestHeaders().getFirst("Origin");
            if (origin == null || origin.isBlank()) {
                response(exchange, "无效请求", HttpStatus.HTTP_BAD_REQUEST);
                return false;
            }

            boolean isAllowed = false;
            for (String allowedOrigin : allowedOrigins) {
                if (origin.equals(allowedOrigin)) {
                    isAllowed = true;
                    break;
                }
            }
            if (!isAllowed) {
                response(exchange, "无效请求", HttpStatus.HTTP_FORBIDDEN);
                return false;
            }
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
        } else {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        }
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "*");
        return true;
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

    private void resetUserLifeTime(String key) {
        ScheduledFuture<?> future = autoRemoveUserMap.get(key);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
        autoRemoveUserMap.put(key, executorService.schedule(() -> {
            autoRemoveUserMap.remove(key);
            userMessageMap.remove(key);
        }, 30, TimeUnit.MINUTES));
    }

    private void removeUser(String key) {
        this.userMessageMap.remove(key);
        final ScheduledFuture<?> future = this.autoRemoveUserMap.remove(key);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }
}