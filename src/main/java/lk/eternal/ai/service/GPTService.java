package lk.eternal.ai.service;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.exception.GPTException;

import java.util.List;

public interface GPTService {

    default GPTResp request(List<Message> messages) throws GPTException {
        return request(messages, null, null);
    }


    default GPTResp request(List<Message> messages, List<String> stop) throws GPTException {
        return request(messages, stop, null);
    }


    GPTResp request(List<Message> messages, List<String> stop, List<Tool> tools) throws GPTException;
}
