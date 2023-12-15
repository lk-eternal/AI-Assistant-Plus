package lk.eternal.ai.plugin;


import cn.hutool.core.math.Calculator;
import lk.eternal.ai.dto.req.Parameters;

import java.util.Map;

public class CalcPlugin implements Plugin {

    @Override
    public String name() {
        return "calc";
    }

    @Override
    public String description() {
        return "数学计算的工具,参数是数学表达式,本工具实际计算使用的是hutool库的Calculator类的conversion方法,请构造出合适的表达式.";
    }

    @Override
    public Parameters parameters() {
        return Parameters.singleton("expression", "string", "数学表达式");
    }

    @Override
    public String execute(Object args) {
        String exp;
        if(args instanceof Map<?,?>){
            exp = ((Map<String, Object>)args).get("expression").toString();
        }else{
            exp = args.toString();
        }
        return String.valueOf(Calculator.conversion(exp));
    }
}
