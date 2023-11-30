package lk.eternal.ai.service;

import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Req;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;


public class ChatGPTService implements GPTService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatGPT4Service.class);

    private final static String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final static HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 1080)))
            .connectTimeout(Duration.ofMinutes(1))
            .build();

    private final String openaiApiKey;
    private final String model;

    public ChatGPTService(String openaiApiKey, String model) {
        this.openaiApiKey = openaiApiKey;
        this.model = model;
    }

    @Override
    public String request(List<Message> messages, List<String> stops) {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + this.openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(Mapper.writeAsStringNotError(new Req(this.model, stops, messages))))
                .build();

        final HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            LOGGER.error("请求OpenAI失败: {}", e.getMessage(), e);
            return e.getMessage();
        }
        final var gptResp = Mapper.readValueNotError(response.body(), GPTResp.class);

        final var error = Optional.ofNullable(gptResp).map(GPTResp::getError).orElse(null);
        if(error != null){
            if("rate_limit_exceeded".equals(error.getCode())){
                return response.statusCode() + ":请求过于频繁,请稍后再试!";
            }
            return Optional.ofNullable(error.getMessage()).orElseGet(() -> response.statusCode() + ":发生未知错误,请稍后再试!");
        }

        return Optional.ofNullable(gptResp)
                .map(GPTResp::getChoices)
                .filter(Predicate.not(List::isEmpty))
                .map(cs -> cs.get(0))
                .map(GPTResp.Choice::getMessage)
                .map(GPTResp.Message::getContent)
                .orElse(response.body());
    }
}
