package lk.eternal.ai.model;

import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.exception.GPTException;
import lk.eternal.ai.service.GPTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class NoneModel implements Model {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoneModel.class);


    private static final int MAX_HISTORY = 10;

    protected final GPTService gptService;

    public NoneModel(GPTService gptService) {
        this.gptService = gptService;
    }

    @Override
    public String getName() {
        return "none";
    }

    @Override
    public String question(LinkedList<Message> messages) {
        LOGGER.info("User: {}", messages.getLast().content());
        var answer = request(messages);
        LOGGER.info("AI: {}", answer);
        messages.addLast(Message.assistant(answer, false));
        return answer;
    }

    protected String request(LinkedList<Message> messages) {
        while (messages.size() > MAX_HISTORY) {
            messages.removeFirst();
        }
        try {
            return this.gptService.request(messages).getContent();
        } catch (GPTException e) {
            return e.getMessage();
        }
    }

}
