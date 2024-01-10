package lk.eternal.ai.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.eternal.ai.domain.User;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.QuestionReq;
import lk.eternal.ai.dto.resp.PluginResp;
import lk.eternal.ai.exception.ApiUnauthorizedException;
import lk.eternal.ai.exception.ApiValidationException;
import lk.eternal.ai.model.ai.AiModel;
import lk.eternal.ai.model.plugin.PluginModel;
import lk.eternal.ai.plugin.Plugin;
import lk.eternal.ai.service.UserService;
import lk.eternal.ai.util.Assert;
import lk.eternal.ai.util.Mapper;
import lk.eternal.ai.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class LKController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LKController.class);

    private final Map<String, Plugin> pluginsMap = new HashMap<>();
    private final Map<String, PluginModel> pluginModelMap = new HashMap<>();
    private final Map<String, AiModel> aiModelMap = new HashMap<>();

    @Resource
    private UserService userService;

    public LKController(List<AiModel> aiModels, List<PluginModel> pluginModels, List<Plugin> plugins) {
        aiModels.forEach(aiModel -> this.aiModelMap.put(aiModel.getName(), aiModel));
        plugins.forEach(plugin -> this.pluginsMap.put(plugin.name(), plugin));
        pluginModels.forEach(pluginModel -> this.pluginModelMap.put(pluginModel.getName(), pluginModel));
    }

    @GetMapping("plugins")
    public List<PluginResp> plugins() {
        return pluginsMap.values().stream()
                .sorted(Comparator.comparing(Plugin::description))
                .map(p -> new PluginResp(p.name(), p.description(), p.properties()))
                .toList();
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
        if (aiModelName.equals("gpt4") && !user.isGpt4Enable()) {
            throw new ApiUnauthorizedException("请输入邀请码");
        }

        final var pluginModelName = Optional.ofNullable(user.getPluginModel())
                .filter(this.pluginModelMap::containsKey)
                .orElse("none");
        if (pluginModelName.equals("native") && !aiModelName.equals("gpt3.5") && !aiModelName.equals("gpt4")) {
            throw new ApiValidationException("通义千问不支持官方原生工具");
        }
        final var plugins = Optional.ofNullable(user.getPlugins())
                .map(ps -> ps.stream().map(this.pluginsMap::get)
                        .filter(Objects::nonNull)
                        .toList())
                .orElseGet(Collections::emptyList);
        final var aiModel = this.aiModelMap.get(aiModelName);

        user.setStatus(User.Status.TYING);
        user.getMessages().addLast(Message.user(questionReq.question()));
        final var modelRole = aiModel.getModelRole();
        user.getMessages().stream()
                .filter(m -> !m.getRole().equals("user") && !m.getRole().equals(modelRole))
                .forEach(m -> m.setRole(modelRole));
        userService.updateUser(user);

        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8");
        response.setStatus(HttpStatus.OK.value());
        final var os = response.getOutputStream();
        this.pluginModelMap.get(pluginModelName).question(aiModel
                , user.getMessages()
                , plugins
                , user::getPluginProperties
                , () -> user.getStatus() == User.Status.STOPPING, resp -> {
                    try {
                        final var respStr = Mapper.writeAsStringNotError(resp);
                        if (respStr != null) {
                            os.write(respStr.getBytes());
                            os.write("[PACKAGE_END]".getBytes());
                            os.flush();
                            response.flushBuffer();
                        }
                    } catch (IOException e) {
                        LOGGER.error("write resp error: {}", e.getMessage(), e);
                    }
                });
        user.setStatus(User.Status.WAITING);
        userService.updateUser(user);
        os.close();
    }

    @PostMapping("stop")
    public void stop(HttpServletRequest request) throws IOException {
        this.userService.getUser(SessionUtil.getSessionId(request))
                .filter(u -> u.getStatus() == User.Status.TYING)
                .ifPresent(user -> user.setStatus(User.Status.STOPPING));
    }

    @DeleteMapping("clear")
    public void clear(HttpServletRequest request) throws IOException {
        this.userService.getUser(SessionUtil.getSessionId(request))
                .ifPresent(user -> {
                    user.clear();
                    userService.updateUser(user);
                });
    }

    private User getUserOrCreate(HttpServletRequest request, HttpServletResponse response) {
        final var sessionId = SessionUtil.getSessionId(request);
        final var user = this.userService.getOrCreateUser(sessionId);
        if (!user.getId().equals(sessionId)) {
            SessionUtil.setSessionId(user.getId(), response);
        }
        return user;
    }
}