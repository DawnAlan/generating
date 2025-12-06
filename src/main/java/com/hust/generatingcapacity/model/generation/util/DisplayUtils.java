package com.hust.generatingcapacity.model.generation.util;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisplayUtils {
    public static String listToStringWithLimit(List<?> list, int limit) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        int size = list.size();
        StringBuilder sb = new StringBuilder("[\n");
        int showCount = Math.min(limit, size);
        for (int i = 0; i < showCount; i++) {
            sb.append("    ").append(list.get(i)).append("\n"); // 缩进4个空格
        }
        if (size > limit) {
            sb.append("    ...还有 ").append(size - limit).append(" 条数据未显示\n");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String getMessageFromExp(String exp, String type) {
        Pattern PATTERN = Pattern.compile(
                "^(?<param>[A-Za-z][A-Za-z0-9]*)" +
                        "\\s*(?<op>>=|<=|>|<|==|\\+|-|\\*|/)" +
                        "\\s*(?<value>" +
                        "-?\\d+(\\.\\d+)?([eE][+\\-]?\\d+)?" +  // ✔ 支持科学计数法
                        "|∞|Infinity|-Infinity|NaN" +
                        "|[A-Za-z][A-Za-z0-9]*" +
                        ")$"
        );
        Matcher m = PATTERN.matcher(exp);
        String param;
        String op;
        String value;
        if (m.matches()) {
            param = m.group("param");
            op = m.group("op");
            value = m.group("value");
            return switch (type) {
                case "param" -> param;
                case "op" -> op;
                case "value" -> value;
                default -> throw new RuntimeException("类型错误，只能是param、op、value");
            };
        } else {
            throw new RuntimeException("公式格式错误，公式中应包含元素参数（例：dH）: " + exp);
        }
    }

    public static Double parseValue(String value, Map<String, Object> env) {
        // 1. 在 env 里
        if (env.containsKey(value)) {
            return (Double) env.get(value);
        }
        // 2. 无穷和 NaN
        if ("∞".equals(value) || "Infinity".equalsIgnoreCase(value)) {
            return Double.POSITIVE_INFINITY;
        }
        if ("-Infinity".equalsIgnoreCase(value)) {
            return Double.NEGATIVE_INFINITY;
        }
        if ("NaN".equalsIgnoreCase(value)) {
            return Double.NaN;
        }
        // 3. 数字
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法解析 value: " + value);
        }
    }


}
