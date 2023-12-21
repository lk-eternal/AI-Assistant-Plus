package lk.eternal.ai.service;

import lk.eternal.ai.dto.req.GeminiReq;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.dto.resp.GeminiResp;
import lk.eternal.ai.exception.GPTException;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GeminiAiModel implements AiModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiAiModel.class);

    private final static HttpClient HTTP_CLIENT;

    static {
        final var builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMinutes(1));
        if (ProxySelector.getDefault() != null) {
            builder.proxy(ProxySelector.getDefault());
        }
        HTTP_CLIENT = builder.build();
    }

    private final String url;
    private final String key;

    public GeminiAiModel(String url, String key) {
        this.url = url;
        this.key = key;
    }

    @Override
    public String getName() {
        return "gemini";
    }

    @Override
    public void request(String prompt, List<Message> messages, List<String> stop, List<Tool> tools, Consumer<GPTResp> respConsumer) throws GPTException {
        final var contents = messages.stream().map(m -> GeminiReq.Content.create(m.getRole(), m.getContent())).collect(Collectors.toList());
        final var requestMessages = new LinkedList<>(contents);
        if (prompt != null) {
            requestMessages.addFirst(GeminiReq.Content.create("user", prompt));
            requestMessages.add(1, GeminiReq.Content.create("model", "好的,我明白了."));
        }
        final var geminiReq = new GeminiReq(requestMessages);
        final var reqStr = Optional.ofNullable(Mapper.writeAsStringNotError(geminiReq))
                .orElseThrow(() -> new GPTException("req can not be null"));

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.url + ":streamGenerateContent?key=" + this.key))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(reqStr))
                .build();

        final HttpResponse<InputStream> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));
            StringBuilder jsonText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (line.equals(",")) {
                    jsonText = new StringBuilder();
                    continue;
                }
                if (line.equals("[{")) {
                    jsonText.append("{");
                } else {
                    jsonText.append(line);
                }
                if (line.equals("}")) {
                    final var geminiResp = Mapper.readValueNotError(jsonText.toString(), GeminiResp.class);
                    if (geminiResp == null) {
                        throw new RuntimeException("Not response");
                    }
                    if (geminiResp.getError() != null) {
                        throw new RuntimeException(geminiResp.getError().getMessage());
                    }
                    respConsumer.accept(new GPTResp(null, null, System.currentTimeMillis(), "gemini"
                            , Optional.ofNullable(geminiResp).map(GeminiResp::getCandidates).orElseGet(Collections::emptyList)
                            .stream()
                            .map(c -> new GPTResp.Choice(1, null, new lk.eternal.ai.dto.req.Message(c.getContent().getRole(), c.getContent().getParts().stream().map(GeminiResp.Part::getText).collect(Collectors.joining()), null, null, null, null), c.getFinishReason()))
                            .collect(Collectors.toList())
                            , null, null, null));
                }
            }
        } catch (Exception e) {
            LOGGER.error("请求Gemini失败: {}", e.getMessage(), e);
            throw new GPTException("请求Gemini失败: " + e.getMessage());
        }
    }

    @Override
    public String getToolRole() {
        return "user";
    }

    @Override
    public String getModelRole() {
        return "model";
    }
}
