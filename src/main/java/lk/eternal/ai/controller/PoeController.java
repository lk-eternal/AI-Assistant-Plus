package lk.eternal.ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.PoeReq;
import lk.eternal.ai.dto.resp.PoeEventResp;
import lk.eternal.ai.exception.ApiUnauthorizedException;
import lk.eternal.ai.model.ai.AiModel;
import lk.eternal.ai.model.ai.ChatGPTAiModel;
import lk.eternal.ai.model.tool.NoneToolModel;
import lk.eternal.ai.util.Assert;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.LinkedList;

@RestController
@RequestMapping("/poe")
public class PoeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PoeController.class);

    private final NoneToolModel toolModel;
    private final AiModel aiModel;
    private final String token;

    public PoeController(@Value("${openai.url}") String openaiApiUrl
            , @Value("${openai.key}") String openaiApiKey
            , @Value("${poe.token}") String poeToken) {
        aiModel = new ChatGPTAiModel(openaiApiKey, openaiApiUrl, "gpt3.5", "gpt-3.5-turbo-1106");
        toolModel = new NoneToolModel();
        token = poeToken;
    }

    @PostMapping
    public void handle(@RequestBody PoeReq appReq, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!request.getHeader("Authorization").equals("Bearer " + token)) {
            throw new ApiUnauthorizedException("无效请求");
        }

        Assert.notNull(appReq, "无效请求");
        LOGGER.info("appReq: {}", Mapper.writeAsStringNotError(appReq));

        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);

        final var messages = new LinkedList<>(appReq.getQuery().stream().map(q -> new Message(q.getRole().equals("bot") ? aiModel.getModelRole() : q.getRole(), q.getContent(), null, null, null, null)).toList());

        response.setStatus(HttpStatus.OK.value());
        final var os = response.getOutputStream();
        this.toolModel.question(this.aiModel, messages, () -> false, resp -> {
            try {
                final var poeEventResp = switch (resp.status()) {
                    case TYPING -> new PoeEventResp("text", new PoeEventResp.TextEvent(resp.message()));
                    case ERROR -> new PoeEventResp("error", new PoeEventResp.ErrorEvent(false, resp.message(), null));
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
    }

}