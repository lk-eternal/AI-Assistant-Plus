package lk.eternal.ai.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MessageManager;
import com.alibaba.dashscope.common.Role;
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
    public void request(String prompt, List<lk.eternal.ai.dto.req.Message> messages, List<String> stop, List<Tool> tools, Consumer<GPTResp> respConsumer) throws GPTException {
        MessageManager msgManager = new MessageManager(10);
        if(prompt != null){
            msgManager.add(Message.builder().role(Role.SYSTEM.getValue()).content(prompt).build());
        }
        final List<Message> msgs = messages.stream().map(m -> Message.builder().role(m.getRole()).content(m.getContent()).build()).collect(Collectors.toList());
        msgs.forEach(msgManager::add);
        QwenParam param = QwenParam.builder().model(Generation.Models.QWEN_PLUS)
                .messages(msgManager.get())
                .resultFormat(QwenParam.ResultFormat.MESSAGE)
                .topP(0.8)
                .enableSearch(false)
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

    @Override
    public String getToolRole() {
        return "user";
    }

    @Override
    public String getModelRole() {
        return "assistant";
    }
}
