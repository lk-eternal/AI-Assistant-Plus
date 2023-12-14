package lk.eternal.ai.service;// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MessageManager;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.utils.Constants;
import io.reactivex.Flowable;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.exception.GPTException;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TongYiQianWenAiModel implements AiModel {

    private final Generation gen;

    public TongYiQianWenAiModel(String tyqwApiKey) {
        Constants.apiKey = tyqwApiKey;
        gen = new Generation();
    }

    @Override
    public String getName() {
        return "tyqw";
    }

    @Override
    public GPTResp request(List<lk.eternal.ai.dto.req.Message> messages, List<String> stop, List<Tool> tools) throws GPTException {
        MessageManager msgManager = new MessageManager(10);
        final List<Message> msgs = messages.stream().map(m -> Message.builder().role(m.getRole()).content(m.getContent()).build()).collect(Collectors.toList());
        msgs.forEach(msgManager::add);
        QwenParam param = QwenParam.builder().model("qwen-max-1201")
                .messages(msgManager.get())
                .resultFormat(QwenParam.ResultFormat.MESSAGE)
                .topP(0.8)
                .enableSearch(true)
                .build();
        try {
            GenerationResult result = gen.call(param);
            return new GPTResp(result.getRequestId(), null, System.currentTimeMillis(), "tyqw"
                    , result.getOutput().getChoices()
                    .stream()
                    .map(c -> new GPTResp.Choice(1, new lk.eternal.ai.dto.req.Message(c.getMessage().getRole(), c.getMessage().getContent(), null, null, null, null), null, c.getFinishReason()))
                    .collect(Collectors.toList())
                    , new GPTResp.Usage(result.getUsage().getInputTokens(), result.getUsage().getOutputTokens(), result.getUsage().getInputTokens() + result.getUsage().getOutputTokens())
                    , null, null);
        } catch (Exception e) {
            throw new GPTException(e.getMessage());
        }
    }

    @Override
    public void request(List<lk.eternal.ai.dto.req.Message> messages, List<String> stop, List<Tool> tools, Consumer<GPTResp> respConsumer) throws GPTException {
        MessageManager msgManager = new MessageManager(10);
        final List<Message> msgs = messages.stream().map(m -> Message.builder().role(m.getRole()).content(m.getContent()).build()).collect(Collectors.toList());
        msgs.forEach(msgManager::add);
        QwenParam param = QwenParam.builder().model(Generation.Models.QWEN_PLUS)
                .messages(msgManager.get())
                .resultFormat(QwenParam.ResultFormat.MESSAGE)
                .topP(0.8)
                .enableSearch(true)
                .incrementalOutput(true)
                .build();
        try {
            Flowable<GenerationResult> result = gen.streamCall(param);
            result.blockingForEach(message -> {
                respConsumer.accept(new GPTResp(message.getRequestId(), null, System.currentTimeMillis(), "tyqw"
                        , message.getOutput().getChoices()
                        .stream()
                        .map(c -> new GPTResp.Choice(1, null, new lk.eternal.ai.dto.req.Message(c.getMessage().getRole(), c.getMessage().getContent(), null, null, null, null), c.getFinishReason()))
                        .collect(Collectors.toList())
                        , new GPTResp.Usage(message.getUsage().getInputTokens(), message.getUsage().getOutputTokens(), message.getUsage().getInputTokens() + message.getUsage().getOutputTokens())
                        , null, null));
            });
        } catch (Exception e) {
            throw new GPTException(e.getMessage());
        }
    }
}
