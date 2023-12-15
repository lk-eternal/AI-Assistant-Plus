package lk.eternal.ai.service;// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MessageManager;
import com.fasterxml.jackson.core.type.TypeReference;
import lk.eternal.ai.dto.req.GeminiReq;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.dto.resp.GeminiResp;
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
    public GPTResp request(List<lk.eternal.ai.dto.req.Message> messages, List<String> stop, List<Tool> tools) throws GPTException {
        final var geminiReq = new GeminiReq(messages.stream().map(m -> GeminiReq.Content.create(m.getRole(), m.getContent())).collect(Collectors.toList()));
        final var reqStr = Optional.ofNullable(Mapper.writeAsStringNotError(geminiReq))
                .orElseThrow(() -> new GPTException("req can not be null"));

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.url + ":generateContent?key=" + this.key))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(reqStr))
                .build();

        final HttpResponse<InputStream> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // 读取返回的流式数据
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));
            StringBuilder jsonText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                LOGGER.info(line);
                if(line.equals(",")){
                    jsonText = new StringBuilder();
                    continue;
                }

                if(line.equals("[{")){
                    jsonText.append("{");
                }else{
                    jsonText.append(line);
                }
                if(line.equals("}")){
                    LOGGER.info(jsonText.toString());
                    final var candidate = Mapper.readValueNotError(jsonText.toString(), new TypeReference<GeminiResp.Candidate>() {
                    });
                    System.out.println(candidate);
                }

//                respConsumer.accept(null);
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.error("请求Gemini失败: {}", e.getMessage(), e);
            throw new GPTException("请求Gemini失败: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void request(List<lk.eternal.ai.dto.req.Message> messages, List<String> stop, List<Tool> tools, Consumer<GPTResp> respConsumer) throws GPTException {
        final var geminiReq = new GeminiReq(messages.stream().map(m -> GeminiReq.Content.create(m.getRole(), m.getContent())).collect(Collectors.toList()));
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

            // 读取返回的流式数据
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));
            StringBuilder jsonText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                LOGGER.info(line);
                if(line.equals(",")){
                    jsonText = new StringBuilder();
                    continue;
                }

                if(line.equals("[{")){
                    jsonText.append("{");
                }else{
                    jsonText.append(line);
                }
                if(line.equals("}")){
                    LOGGER.info(jsonText.toString());
                    final var geminiResp = Mapper.readValueNotError(jsonText.toString(), new TypeReference<GeminiResp>() {
                    });
                    System.out.println(geminiResp);
                }

//                respConsumer.accept(null);
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.error("请求Gemini失败: {}", e.getMessage(), e);
            throw new GPTException("请求Gemini失败: " + e.getMessage());
        }
    }
}
