package lk.eternal.ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lk.eternal.ai.domain.User;
import lk.eternal.ai.dto.req.AppReq;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.exception.ApiUnauthorizedException;
import lk.eternal.ai.exception.ApiValidationException;
import lk.eternal.ai.model.ai.AiModel;
import lk.eternal.ai.model.ai.ChatGPTAiModel;
import lk.eternal.ai.model.ai.GeminiAiModel;
import lk.eternal.ai.model.ai.TongYiQianWenAiModel;
import lk.eternal.ai.model.tool.*;
import lk.eternal.ai.plugin.CalcPlugin;
import lk.eternal.ai.plugin.GoogleSearchPlugin;
import lk.eternal.ai.plugin.HttpPlugin;
import lk.eternal.ai.plugin.Plugin;
import lk.eternal.ai.util.Assert;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private final Map<String, ToolModel> toolModelMap = new HashMap<>();
    private final Map<String, AiModel> aiModelMap = new HashMap<>();

    public LKController(@Value("${openai.url}") String openaiApiUrl
            , @Value("${openai.key}") String openaiApiKey
            , @Value("${tyqw.key}") String tyqwApiKey
            , @Value("${google.gemini.key}") String geminiApiKey
            , @Value("${google.search.key}") String googleSearchKey
            , @Value("${google.search.cx}") String googleSearchCx
            , @Value("${ssh.username}") String sshUsername
            , @Value("${ssh.password}") String sshPassword
            , @Value("${ssh.host}") String sshHost
            , @Value("${ssh.port}") Integer sshPort
    ) {
        final var chatGPT35Service = new ChatGPTAiModel(openaiApiKey, openaiApiUrl, "gpt3.5", "gpt-3.5-turbo-1106");
        final var chatGPT4Service = new ChatGPTAiModel(openaiApiKey, openaiApiUrl, "gpt4", "gpt-4-1106-preview");
        final var tyqwService = new TongYiQianWenAiModel(tyqwApiKey);
        final var geminiService = new GeminiAiModel(geminiApiKey);
        this.aiModelMap.put(chatGPT35Service.getName(), chatGPT35Service);
        this.aiModelMap.put(chatGPT4Service.getName(), chatGPT4Service);
        this.aiModelMap.put(tyqwService.getName(), tyqwService);
        this.aiModelMap.put(geminiService.getName(), geminiService);

        final var plugins = new ArrayList<Plugin>();
        plugins.add(new CalcPlugin());
        plugins.add(new HttpPlugin());
        plugins.add(new GoogleSearchPlugin(googleSearchKey, googleSearchCx));
//        plugins.add(new DbPlugin());
//        plugins.add(new SshPlugin(sshUsername, sshPassword, sshHost, sshPort));
//        plugins.add(new CmdPlugin());

        BaseToolModel toolModel = new NativeToolModel();
        plugins.forEach(toolModel::addPlugin);
        this.toolModelMap.put(toolModel.getName(), toolModel);

        BaseToolModel cmdPluginModel = new CmdToolModel();
        plugins.forEach(cmdPluginModel::addPlugin);
        this.toolModelMap.put(cmdPluginModel.getName(), cmdPluginModel);

        BaseToolModel formatPluginModel = new FormatToolModel();
        plugins.forEach(formatPluginModel::addPlugin);
        this.toolModelMap.put(formatPluginModel.getName(), formatPluginModel);

        NoneToolModel noneModel = new NoneToolModel();
        this.toolModelMap.put(noneModel.getName(), noneModel);
    }

    @PostMapping("question")
    public void question(@RequestBody AppReq appReq, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Assert.notNull(appReq, "无效请求");
        LOGGER.info("appReq: {}", Mapper.writeAsStringNotError(appReq));

        var sessionId = getOrCreateSessionId(request);
        resetUserLifeTime(sessionId);

        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);

        final var user = this.userMap.computeIfAbsent(sessionId, k -> new User(User.Status.WAITING, new LinkedList<>()));
        Assert.isTrue(user.getStatus() == User.Status.WAITING, "请等待上次回答完成...");
        final var aiModelName = Optional.ofNullable(appReq.aiModel())
                .filter(this.aiModelMap::containsKey)
                .orElse("tyqw");
        final var toolModelName = Optional.ofNullable(appReq.toolModel())
                .filter(this.toolModelMap::containsKey)
                .orElse("none");

        if (aiModelName.equals("gpt4") && !"lk123".equals(appReq.gpt4Code())) {
            throw new ApiUnauthorizedException("邀请码不正确");
        }
        if (toolModelName.equals("native") && !aiModelName.equals("gpt3.5") && !aiModelName.equals("gpt4")) {
            throw new ApiValidationException("通义千问不支持官方原生工具");
        }
        user.setStatus(User.Status.TYING);
        user.getMessages().addLast(Message.user(appReq.question()));
        response.setStatus(HttpStatus.OK.value());
        final var os = response.getOutputStream();
        this.toolModelMap.get(toolModelName).question(this.aiModelMap.get(aiModelName), user.getMessages()
                , () -> user.getStatus() == User.Status.STOPPING
                , resp -> {
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
        var sessionId = getOrCreateSessionId(request);
        final var user = this.userMap.computeIfAbsent(sessionId, k -> new User(User.Status.WAITING, new LinkedList<>()));
        if (user.getStatus() == User.Status.TYING) {
            user.setStatus(User.Status.STOPPING);
        }
    }

    @DeleteMapping("clear")
    public void clear(HttpServletRequest request) throws IOException {
        var sessionId = getOrCreateSessionId(request);
        if (sessionId != null) {
            removeUser(sessionId);
        }
    }

    private String getOrCreateSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession();
        }
        session.setMaxInactiveInterval(30 * 60);
        return session.getId();
    }

    private void resetUserLifeTime(String key) {
        ScheduledFuture<?> future = autoRemoveUserMap.get(key);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
        autoRemoveUserMap.put(key, executorService.schedule(() -> {
            autoRemoveUserMap.remove(key);
            userMap.remove(key);
        }, 30, TimeUnit.MINUTES));
    }

    private void removeUser(String key) {
        this.userMap.remove(key);
        final ScheduledFuture<?> future = this.autoRemoveUserMap.remove(key);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }
}