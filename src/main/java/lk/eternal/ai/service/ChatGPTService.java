package lk.eternal.ai.service;

import lk.eternal.ai.dto.req.GPTReq;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.exception.GPTException;
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


public class ChatGPTService implements GPTService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatGPT4Service.class);

    private static final HttpClient HTTP_CLIENT;

    static {
        final var builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMinutes(1));
        if (ProxySelector.getDefault() != null) {
            builder.proxy(ProxySelector.getDefault());
        }
        HTTP_CLIENT = builder.build();
    }

    private final String openaiApiKey;
    private final String openaiApiUrl;
    private final String model;

    public ChatGPTService(String openaiApiKey, String openaiApiUrl, String model) {
        this.openaiApiKey = openaiApiKey;
        this.openaiApiUrl = openaiApiUrl;
        this.model = model;
    }

    @Override
    public GPTResp request(List<Message> messages, List<String> stop, List<Tool> tools) throws GPTException {
        final var reqStr = Optional.ofNullable(Mapper.writeAsStringNotError(new GPTReq(this.model, messages, stop, tools)))
                .orElseThrow(() -> new GPTException("req can not be null"));

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.openaiApiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + this.openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(reqStr))
                .build();

        final HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            LOGGER.error("请求OpenAI失败: {}", e.getMessage(), e);
            throw new GPTException("请求OpenAI失败: " + e.getMessage());
        }
        final var gptResp = Mapper.readValueNotError(response.body(), GPTResp.class);

        final var error = Optional.ofNullable(gptResp).map(GPTResp::error).orElse(null);
        if (error != null) {
            if ("rate_limit_exceeded".equals(error.code())) {
                throw new GPTException(response.statusCode() + ":请求过于频繁,请稍后再试!");
            }
            throw new GPTException(Optional.ofNullable(error.message())
                    .orElseGet(() -> response.statusCode() + ":发生未知错误,请稍后再试!"));
        }
        return gptResp;
    }
}
