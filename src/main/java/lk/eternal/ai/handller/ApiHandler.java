package lk.eternal.ai.handller;

import cn.hutool.http.HttpStatus;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lk.eternal.ai.dto.req.AppReq;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.model.*;
import lk.eternal.ai.plugin.*;
import lk.eternal.ai.service.AiModel;
import lk.eternal.ai.service.ChatGPTAiModel;
import lk.eternal.ai.service.GeminiAiModel;
import lk.eternal.ai.service.TongYiQianWenAiModel;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.util.*;

public class ApiHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiHandler.class);

    public record User(String id, LinkedList<Message> messages) {
    }

    private final Map<String, User> userMap = new HashMap<>();
    private final Map<String, ToolModel> toolModelMap = new HashMap<>();
    private final Map<String, AiModel> aiModelMap = new HashMap<>();

    public ApiHandler() {
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

        final var calcPlugin = new CalcPlugin();
        final var dbPlugin = new DbPlugin();
        final var httpPlugin = new HttpPlugin();
        final var googleSearchPlugin = new GoogleSearchPlugin(System.getProperty("google.key"), System.getProperty("google.search.cx"));
        final var sshPlugin = new SshPlugin(System.getProperty("ssh.username"), System.getProperty("ssh.password"), System.getProperty("ssh.host"), Integer.parseInt(System.getProperty("ssh.port")));
        final var cmdPlugin = new CmdPlugin();

        BaseToolModel toolModel = new NativeToolModel();
        toolModel.addPlugin(calcPlugin);
        toolModel.addPlugin(httpPlugin);
        toolModel.addPlugin(googleSearchPlugin);
//            toolModel.addPlugin(dbPlugin);
//            toolModel.addPlugin(sshPlugin);
//            toolModel.addPlugin(cmdPlugin);
        this.toolModelMap.put(toolModel.getName(), toolModel);

        BaseToolModel cmdPluginModel = new CmdToolModel();
        cmdPluginModel.addPlugin(calcPlugin);
        cmdPluginModel.addPlugin(httpPlugin);
        cmdPluginModel.addPlugin(googleSearchPlugin);
//            cmdPluginModel.addPlugin(dbPlugin);
//            cmdPluginModel.addPlugin(sshPlugin);
//            cmdPluginModel.addPlugin(cmdPlugin);
        this.toolModelMap.put(cmdPluginModel.getName(), cmdPluginModel);

        BaseToolModel formatPluginModel = new FormatToolModel();
        formatPluginModel.addPlugin(calcPlugin);
        formatPluginModel.addPlugin(httpPlugin);
        formatPluginModel.addPlugin(googleSearchPlugin);
//            formatPluginModel.addPlugin(dbPlugin);
//            formatPluginModel.addPlugin(sshPlugin);
//            formatPluginModel.addPlugin(cmdPlugin);
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
            LOGGER.info(req);
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

            user.messages().addLast(Message.user(appReq.question()));

            t.getResponseHeaders().set("Content-Type", "text/event-stream");
            t.getResponseHeaders().set("Cache-Control", "no-cache");
            t.getResponseHeaders().set("Connection", "keep-alive");
            t.sendResponseHeaders(200, 0);
            OutputStream os = t.getResponseBody();
            this.toolModelMap.get(toolModelName).question(this.aiModelMap.get(aiModelName), user.messages(), resp -> {
                try {
                    final var respStr = Mapper.writeAsStringNotError(resp);
                    if (respStr != null) {
                        os.write(respStr.getBytes());
                        os.write("\n".getBytes());
                        os.flush();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
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