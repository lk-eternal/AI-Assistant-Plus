package lk.eternal.ai.handller;

import cn.hutool.http.HttpStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.PoeReq;
import lk.eternal.ai.dto.resp.PoeEventResp;
import lk.eternal.ai.model.ai.AiModel;
import lk.eternal.ai.model.ai.ChatGPTAiModel;
import lk.eternal.ai.model.tool.NoneToolModel;
import lk.eternal.ai.util.Mapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

public class PoeHandler implements HttpHandler {

    private final NoneToolModel toolModel;
    private final AiModel aiModel;
    private final String token;

    public PoeHandler() {
        final var openaiApiUrl = System.getProperty("openai.url");
        final var openaiApiKey = System.getProperty("openai.key");
        aiModel = new ChatGPTAiModel(openaiApiKey, openaiApiUrl, "gpt3.5", "gpt-3.5-turbo-1106");
        toolModel = new NoneToolModel();
        token = System.getProperty("poe.token");
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
        t.getResponseHeaders().add("Access-Control-Allow-Methods", "*");

        if (t.getRequestMethod().equalsIgnoreCase("post")) {
            if (!t.getRequestHeaders().get("Authorization").get(0).equals("Bearer " + token)) {
                response(t, "无效请求", HttpStatus.HTTP_UNAUTHORIZED);
                return;
            }

            final var req = new String(t.getRequestBody().readAllBytes());
            final var appReq = Mapper.readValueNotError(req, PoeReq.class);
            if (appReq == null) {
                response(t, "无效请求", HttpStatus.HTTP_BAD_REQUEST);
                return;
            }

            final var messages = new LinkedList<>(appReq.getQuery().stream().map(q -> new Message(q.getRole().equals("bot") ? aiModel.getModelRole() : q.getRole(), q.getContent(), null, null, null, null)).toList());

            t.getResponseHeaders().set("Content-Type", "text/event-stream");
            t.getResponseHeaders().set("Cache-Control", "no-cache");
            t.getResponseHeaders().set("Connection", "keep-alive");
            t.sendResponseHeaders(200, 0);
            OutputStream os = t.getResponseBody();
            this.toolModel.question(this.aiModel, messages, resp -> {
                try {
                    final var poeEventResp = switch (resp.status()) {
                        case TYPING -> new PoeEventResp("text", new PoeEventResp.TextEvent(resp.message()));
                        case ERROR ->
                                new PoeEventResp("error", new PoeEventResp.ErrorEvent(false, resp.message(), null));
                        case FUNCTION_CALLING -> null;
                    };
                    if (poeEventResp != null) {
                        os.write(("event: " + poeEventResp.event()).getBytes());
                        os.write("\n".getBytes());
                        os.write(("data: " + Mapper.writeAsStringNotError(poeEventResp.data())).getBytes());
                        os.write("\n\n".getBytes());
                        os.flush();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            os.write(("event: done").getBytes());
            os.write("\n".getBytes());
            os.write(("data: {}").getBytes());
            os.write("\n\n".getBytes());
            os.flush();
            os.close();
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
}