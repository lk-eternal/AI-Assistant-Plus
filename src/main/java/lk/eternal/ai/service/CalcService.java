package lk.eternal.ai.service;


import cn.hutool.core.math.Calculator;

public class CalcService implements Service {

    @Override
    public String name() {
        return "calc";
    }

    @Override
    public String description() {
        return "数学计算的工具,输入是数学表达式,本工具实际计算使用的是hutool库的Calculator类的conversion方法,请构造出合适的表达式.";
    }

    public String execute(String param) {
        return String.valueOf(Calculator.conversion(param));
    }
}
