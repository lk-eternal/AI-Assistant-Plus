package lk.eternal.ai.controller;


import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.eternal.ai.domain.User;
import lk.eternal.ai.dto.req.RegisterLoginReq;
import lk.eternal.ai.dto.resp.UserResp;
import lk.eternal.ai.exception.ApiUnauthorizedException;
import lk.eternal.ai.service.UserService;
import lk.eternal.ai.util.Assert;
import lk.eternal.ai.util.SessionUtil;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("register")
    public void register(@RequestBody RegisterLoginReq req, HttpServletRequest request, HttpServletResponse response) {
        final var user = getUser(request, response);
        if (user.getEmail() != null) {
            return;
        }
        userService.createUser(user, req.email(), req.password());
    }

    @PostMapping("refresh")
    public UserResp refresh(HttpServletRequest request) {
        final var user = userService.getUser(SessionUtil.getSessionId(request))
                .orElseThrow(() -> new ApiUnauthorizedException("未登录"));
        return new UserResp(user);
    }

    @PostMapping("login")
    public UserResp login(@RequestBody RegisterLoginReq req, HttpServletRequest request, HttpServletResponse response) {
        final var user = userService.getUserByEmail(req.email(), req.password())
                .orElseThrow(() -> new ApiUnauthorizedException("用户名或密码错误"));
        if(!Boolean.TRUE.equals(req.loadHistory())){
            user.clear();

            //如果当前有对话则放到用户上
            Optional.ofNullable(SessionUtil.getSessionId(request))
                    .flatMap(userService::getUser)
                    .ifPresent(u -> {
                        user.setMessages(u.getMessages());
                        userService.deleteUser(u.getId());
                        userService.updateUser(user);
                    });
        }
        SessionUtil.setSessionId(user, response);
        return new UserResp(user);
    }

    @PostMapping("exit")
    public void exit(HttpServletRequest request) throws IOException {
        this.userService.getUser(SessionUtil.getSessionId(request))
                .filter(u -> !u.isDbUser())
                .ifPresent(user -> userService.deleteUser(user.getId()));
    }

    @PostMapping("valid/gpt4/{code}")
    public void validGpt4(@PathVariable String code, HttpServletRequest request) {
        Assert.isTrue(code.equals("lk123"), "code is invalid");
        this.userService.getUser(SessionUtil.getSessionId(request))
                .ifPresent(user -> {
                    user.setGpt4Enable(true);
                    userService.updateUser(user);
                });
    }

    @PutMapping("properties")
    public void properties(@RequestBody Map<String, Object> properties, HttpServletRequest request, HttpServletResponse response) {
        final var user = getUser(request, response);
        properties.forEach(user::putProperty);
        userService.updateUser(user);
    }

    private User getUser(HttpServletRequest request, HttpServletResponse response) {
        final var sessionId = SessionUtil.getSessionId(request);
        final var user = this.userService.getOrCreateUser(sessionId);
        if (!user.getId().equals(sessionId)) {
            SessionUtil.setSessionId(user, response);
        }
        return user;
    }
}
