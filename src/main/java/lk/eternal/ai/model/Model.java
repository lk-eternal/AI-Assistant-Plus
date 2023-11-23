package lk.eternal.ai.model;


import lk.eternal.ai.service.Service;

public interface Model {

    String question(String question);

    void addService(Service service);
}
