package lk.eternal.ai.plugin;


import cn.hutool.core.math.Calculator;
import lk.eternal.ai.dto.req.Parameters;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class CalcPlugin implements Plugin {

    @Override
    public String name() {
        return "calc";
    }

    @Override
    public String prompt() {
        return "A tool for performing mathematical calculations. This tool uses the conversion method of the Calculator class in the hutool library to perform actual calculations and execute corresponding calculation tasks by receiving mathematical expressions from users. When using this tool, please ensure that your input is a valid mathematical expression to obtain the correct result.";
    }

    @Override
    public String description() {
        return "1.计算器";
    }

    @Override
    public Parameters parameters() {
        return Parameters.singleton("expression", "string", "数学表达式");
    }

    @Override
    public String execute(Map<String, Object> args) {
        String exp = Optional.ofNullable(args.get("expression"))
                .orElseGet(() -> args.get("value"))
                .toString();
        return String.valueOf(Calculator.conversion(exp));
    }
}
