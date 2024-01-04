package lk.eternal.ai.dto.req;


import lk.eternal.ai.util.Assert;

public record RegisterLoginReq(String email, String password, Boolean loadHistory) {
    public RegisterLoginReq {
        Assert.hasText(email, "email can not be empty");
        Assert.hasText(password, "password can not be empty");
    }
}
