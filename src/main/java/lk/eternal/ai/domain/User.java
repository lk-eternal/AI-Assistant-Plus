package lk.eternal.ai.domain;

import lk.eternal.ai.dto.req.Message;

import java.util.LinkedList;

public class User {
    private Status status;
    private LinkedList<Message> messages;

    public User(Status status, LinkedList<Message> messages) {
        this.status = status;
        this.messages = messages;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }

    public LinkedList<Message> getMessages() {
        return messages;
    }

    public void setMessages(LinkedList<Message> messages) {
        this.messages = messages;
    }

    public enum Status{
        TYING,
        STOPPING,
        WAITING
    }
}
