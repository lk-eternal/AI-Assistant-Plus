package lk.eternal.ai.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lk.eternal.ai.domain.User;
import lk.eternal.ai.dto.req.QuestionReq;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.resp.PluginResp;
import lk.eternal.ai.exception.ApiUnauthorizedException;
import lk.eternal.ai.exception.ApiValidationException;
import lk.eternal.ai.model.ai.AiModel;
import lk.eternal.ai.model.plugin.*;
import lk.eternal.ai.plugin.*;
import lk.eternal.ai.util.Assert;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api")
public class LKController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LKController.class);

    private final Map<String, User> userMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ScheduledFuture<?>> autoRemoveUserMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private final Map<String, Plugin> pluginsMap = new HashMap<>();
    private final Map<String, PluginModel> pluginModelMap = new HashMap<>();
    private final Map<String, AiModel> aiModelMap = new HashMap<>();

    public LKController(List<AiModel> aiModels, List<PluginModel> pluginModels, List<Plugin> plugins) {
        aiModels.forEach(aiModel -> this.aiModelMap.put(aiModel.getName(), aiModel));
        plugins.forEach(plugin -> this.pluginsMap.put(plugin.name(), plugin));
        pluginModels.forEach(pluginModel -> this.pluginModelMap.put(pluginModel.getName(), pluginModel));
    }

    @PostMapping("question")
    public void question(@RequestBody QuestionReq questionReq, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Assert.notNull(questionReq, "无效请求");
        LOGGER.info("questionReq: {}", Mapper.writeAsStringNotError(questionReq));

        var user = getUserOrCreate(request, response);
        Assert.isTrue(user.getStatus() == User.Status.WAITING, "请等待上次回答完成...");

        final var aiModelName = Optional.ofNullable(user.getAiModel())
                .filter(this.aiModelMap::containsKey)
                .orElse("tyqw");
        if (aiModelName.equals("gpt4") && !"lk123".equals(user.getGpt4Code())) {
            throw new ApiUnauthorizedException("邀请码不正确");
        }

        final var pluginModelName = Optional.ofNullable(user.getPluginModel())
                .filter(this.pluginModelMap::containsKey)
                .orElse("none");
        if (pluginModelName.equals("native") && !aiModelName.equals("gpt3.5") && !aiModelName.equals("gpt4")) {
            throw new ApiValidationException("通义千问不支持官方原生工具");
        }
        user.setStatus(User.Status.TYING);
        user.getMessages().addLast(Message.user(questionReq.question()));

        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setStatus(HttpStatus.OK.value());
        final var os = response.getOutputStream();
        this.pluginModelMap.get(pluginModelName).question(this.aiModelMap.get(aiModelName)
                , user.getMessages()
                , user.getPlugins().stream().map(this.pluginsMap::get).filter(Objects::nonNull).toList()
                , user::getPluginProperties
                , () -> user.getStatus() == User.Status.STOPPING, resp -> {
                    try {
                        final var respStr = Mapper.writeAsStringNotError(resp);
                        if (respStr != null) {
                            os.write(respStr.getBytes());
                            os.write("[PACKAGE_END]".getBytes());
                            os.flush();
                        }
                    } catch (IOException e) {
                        LOGGER.error("write resp error: {}", e.getMessage(), e);
                    }
                });
        user.setStatus(User.Status.WAITING);
        os.close();
    }

    @PostMapping("stop")
    public void stop(HttpServletRequest request) throws IOException {
        final var user = getUser(request);
        if (user != null && user.getStatus() == User.Status.TYING) {
            user.setStatus(User.Status.STOPPING);
        }
    }

    @DeleteMapping("clear")
    public void clear(HttpServletRequest request) throws IOException {
        final var user = getUser(request);
        if (user != null) {
            user.clear();
        }
    }

    @PostMapping("exit")
    public void exit(HttpServletRequest request) throws IOException {
        final var user = getUser(request);
        if (user != null) {
            removeUser(user.getId());
        }
    }


    @PutMapping("properties")
    public void properties(@RequestBody Map<String, Object> properties, HttpServletRequest request, HttpServletResponse response){
        var user = getUserOrCreate(request, response);
        properties.forEach(user::putProperty);
    }

    @GetMapping("plugins")
    public List<PluginResp> plugins(){
        return pluginsMap.values().stream()
                .sorted(Comparator.comparing(Plugin::description))
                .map(p -> new PluginResp(p.name(), p.description(), p.properties()))
                .toList();
    }

    private User getUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return this.userMap.get(session.getId());
    }

    private User getUserOrCreate(HttpServletRequest request, HttpServletResponse response) {
        final var user = Optional.ofNullable(getUser(request))
                .orElseGet(() -> {
                    final var session = request.getSession();
                    session.setMaxInactiveInterval(30 * 60);
                    final var sessionId = session.getId();

                    Cookie sessionCookie = new Cookie("JSESSIONID", sessionId);
                    sessionCookie.setSecure(true);
                    sessionCookie.setHttpOnly(true);
                    sessionCookie.setAttribute("SameSite", "None");
                    response.addCookie(sessionCookie);

                    final var u = new User(sessionId);
                    this.userMap.put(sessionId, u);
                    return u;
                });
        resetUserLifeTime(user.getId());
        return user;
    }

    private void resetUserLifeTime(String sessionId) {
        ScheduledFuture<?> future = autoRemoveUserMap.get(sessionId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
        autoRemoveUserMap.put(sessionId, executorService.schedule(() -> {
            autoRemoveUserMap.remove(sessionId);
            userMap.remove(sessionId);
        }, 30, TimeUnit.MINUTES));
    }

    private void removeUser(String sessionId) {
        this.userMap.remove(sessionId);
        final ScheduledFuture<?> future = this.autoRemoveUserMap.remove(sessionId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }
}