package lk.eternal.ai.plugin;

import lk.eternal.ai.dto.req.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CmdPlugin implements Plugin {

    @Override
    public String name() {
        return "cmd";
    }

    @Override
    public String description() {
        return "A tool for executing cmd commands. This tool can run in a Windows 11 environment and execute corresponding operations by receiving commands from users. When using this tool, please do not enter any commands that may pose a threat to system security.";
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

        StringBuilder result = new StringBuilder();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", exp);
            processBuilder.redirectErrorStream(true);
            processBuilder.environment().put("LANG", "zh_CN.UTF-8");
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            boolean completed = process.waitFor(2, TimeUnit.SECONDS);
            if (!completed) {
                process.destroy();
            }
            return result.toString();
        } catch (IOException | InterruptedException e) {
            return result.toString();
        }
    }
}
