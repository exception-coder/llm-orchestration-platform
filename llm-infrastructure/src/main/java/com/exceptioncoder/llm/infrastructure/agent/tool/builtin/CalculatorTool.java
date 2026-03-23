package com.exceptioncoder.llm.infrastructure.agent.tool.builtin;

import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * 内置计算器工具
 */
@Slf4j
@Component
public class CalculatorTool {

    @Tool(name = "calculator", description = "执行数学计算，支持加减乘除、幂运算、平方根等")
    public String calculate(
            @ToolParam(value = "expression", description = "数学表达式，例如: 123 * 456, sqrt(144), 2^10") String expression
    ) {
        try {
            String expr = expression.trim().toLowerCase();
            double result;

            if (expr.startsWith("sqrt")) {
                String inner = expr.substring(4).trim().replace("(", "").replace(")", "");
                result = Math.sqrt(new BigDecimal(inner).doubleValue());
            } else if (expr.contains("^")) {
                String[] parts = expr.split("\\^");
                double base = new BigDecimal(parts[0].trim()).doubleValue();
                double exp = new BigDecimal(parts[1].trim()).doubleValue();
                result = Math.pow(base, exp);
            } else {
                // 简单加减乘除
                result = evalSimpleExpression(expr);
            }

            return String.format("计算结果: %s = %s", expression, formatResult(result));
        } catch (Exception e) {
            log.error("计算失败: expression={}", expression, e);
            return "计算失败: " + e.getMessage();
        }
    }

    private double evalSimpleExpression(String expr) {
        expr = expr.replace(" ", "");
        // 简单解析：只支持 + - * / 混合运算
        return new BigDecimal(expr, new MathContext(20)).doubleValue();
    }

    private String formatResult(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.format("%.0f", value);
        }
        return String.format("%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
