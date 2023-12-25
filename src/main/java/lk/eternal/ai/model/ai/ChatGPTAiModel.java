package lk.eternal.ai.model.ai;

import lk.eternal.ai.dto.req.GPTReq;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.exception.GPTException;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;


public class ChatGPTAiModel implements AiModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatGPTAiModel.class);

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
    private final String name;
    private final String model;

    public ChatGPTAiModel(String openaiApiKey, String openaiApiUrl, String name, String model) {
        this.openaiApiKey = openaiApiKey;
        this.openaiApiUrl = openaiApiUrl;
        this.name = name;
        this.model = model;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void request(String prompt, List<Message> messages, List<String> stop, List<Tool> tools, Consumer<GPTResp> respConsumer) throws GPTException {
        final var requestMessages = new LinkedList<>(messages);
        if (prompt != null) {
            requestMessages.addFirst(Message.create("system", prompt, false));
        }
        final var gptReq = new GPTReq(this.model, requestMessages, stop, tools, true);
        final var reqStr = Optional.ofNullable(Mapper.writeAsStringNotError(gptReq))
                .orElseThrow(() -> new GPTException("req can not be null"));

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.openaiApiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + this.openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(reqStr))
                .build();

        final HttpResponse<InputStream> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // 读取返回的流式数据
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                final var gptResp = Mapper.readValueNotError(line.substring(line.indexOf("{")), GPTResp.class);
                if (gptResp == null) {
                    continue;
                }
                if (gptResp.choices().get(0).getFinish_reason() != null) {
                    break;
                }
                respConsumer.accept(gptResp);
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.error("请求OpenAI失败: {}", e.getMessage(), e);
            throw new GPTException("请求OpenAI失败: " + e.getMessage());
        }
    }

    @Override
    public String getToolRole() {
        return "system";
    }

    @Override
    public String getModelRole() {
        return "assistant";
    }
}
