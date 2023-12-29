package lk.eternal.ai.plugin;

import com.jcraft.jsch.*;
import lk.eternal.ai.dto.req.Parameters;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SshPlugin implements Plugin {

    @Override
    public String name() {
        return "ssh";
    }

    @Override
    public String prompt() {
        return "A tool for executing shell commands. This tool can run in a Linux environment and execute corresponding operations by receiving commands from users. When using this tool, please do not enter any commands that may pose a threat to system security.";
    }

    @Override
    public String description() {
        return "5.Shell终端";
    }

    @Override
    public List<Prop> properties() {
        return List.of(new Prop("host", "host")
                ,new Prop("port", "port")
                ,new Prop("username", "username")
                ,new Prop("password", "password"));
    }

    @Override
    public Parameters parameters() {
        return Parameters.singleton("exec", "string", "执行命令");
    }

    @Override
    public String execute(Map<String, Object> args) {
        String exp = Optional.ofNullable(args.get("exec"))
                .orElseGet(() -> args.get("value"))
                .toString();

        try {
            JSch jsch = new JSch();

            // 创建SSH会话
            Session session = jsch.getSession(args.get("username").toString(), args.get("host").toString(), Integer.parseInt(args.get("port").toString()));
            session.setPassword(args.get("password").toString());

            // 配置SSH连接选项
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            // 连接SSH服务器
            session.connect();

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
