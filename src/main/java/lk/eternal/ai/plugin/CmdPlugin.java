package lk.eternal.ai.plugin;

import lk.eternal.ai.dto.req.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class CmdPlugin implements Plugin {

    @Override
    public String name() {
        return "cmd";
    }

    @Override
    public String description() {
        return "执行cmd命令的工具,参数是执行命令,机器环境是windows11(本机),禁止执行对系统安全有威胁的命令";
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
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", exp);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            process.waitFor();
            return result.toString();
        } catch (IOException | InterruptedException e) {
            return e.getMessage();
        }
    }
}
