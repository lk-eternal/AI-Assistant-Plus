package lk.eternal.ai.plugin;

import com.jcraft.jsch.*;
import lk.eternal.ai.dto.req.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class SshPlugin implements Plugin {

    private final Session session;

    public SshPlugin(String username, String password, String host, Integer port) {
        try {
            JSch jsch = new JSch();

            // 创建SSH会话
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            // 配置SSH连接选项
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            // 连接SSH服务器
            session.connect();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return "ssh";
    }

    @Override
    public String description() {
        return "执行shell命令的工具,参数是执行命令,机器环境是linux(服务器),禁止执行对系统安全有威胁的命令";
    }

    @Override
    public Parameters parameters() {
        return Parameters.singleton("exec", "string", "执行命令");
    }

    @Override
    public String execute(Object args) {
        String exp;
        if (args instanceof Map<?, ?>) {
            exp = ((Map<String, Object>) args).get("exec").toString();
        } else {
            exp = args.toString();
        }

        try {
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(exp);

            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] buffer = new byte[1024];
            StringBuilder result = new StringBuilder();
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                result.append(new String(buffer, 0, bytesRead));
            }

            channel.disconnect();
            return result.toString();
        } catch (JSchException | IOException e) {
            return e.getMessage();
        }
    }
}
