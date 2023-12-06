package lk.eternal.ai.service;// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MessageManager;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.exception.GPTException;

import java.util.List;
import java.util.stream.Collectors;

public class TongYiQianWenService implements GPTService {

    private final Generation gen;

    public TongYiQianWenService(String tyqwApiKey) {
        Constants.apiKey = tyqwApiKey;
        gen = new Generation();
    }

    @Override
    public GPTResp request(List<lk.eternal.ai.dto.req.Message> messages, List<String> stop, List<Tool> tools) throws GPTException {
        MessageManager msgManager = new MessageManager(10);
        final List<Message> msgs = messages.stream().map(m -> Message.builder().role(m.role()).content(m.content()).build()).collect(Collectors.toList());
        msgs.forEach(msgManager::add);
        QwenParam param = QwenParam.builder().model(Generation.Models.QWEN_PLUS)
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
                    .map(c -> new GPTResp.Choice(1, new lk.eternal.ai.dto.req.Message(c.getMessage().getRole(), c.getMessage().getContent(), null, null, null, null), c.getFinishReason()))
                    .collect(Collectors.toList())
                    , new GPTResp.Usage(result.getUsage().getInputTokens(), result.getUsage().getOutputTokens(), result.getUsage().getInputTokens() + result.getUsage().getOutputTokens())
                    , null, null);
        } catch (Exception e) {
            throw new GPTException(e.getMessage());
        }
    }
}
