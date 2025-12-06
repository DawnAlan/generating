package com.hust.generatingcapacity.model.generation.domain;

import com.googlecode.aviator.AviatorEvaluator;
import com.hust.generatingcapacity.model.generation.type.ParamBoundType;
import com.hust.generatingcapacity.model.generation.type.ParamType;
import com.hust.generatingcapacity.model.generation.util.DisplayUtils;
import com.hust.generatingcapacity.model.generation.vo.BoundPair;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.*;


@Data
@Getter
@Setter
public class ConstraintData {
    //约束类型
    private String constraintType;
    //是否为硬约束
    private Boolean rigid;
    //约束描述
    private String description;
    //生效条件
    private String condition;
    //约束参数
    private List<String> param;

    /**
     * 判断条件是否生效
     *
     * @param condition
     * @param env
     * @return
     */
    public Boolean isConditionActive(String condition, Map<String, Object> env) {
        if (condition == null || condition.isEmpty()) {
            return false;
        }
        if (env == null || env.isEmpty()) {
            throw new IllegalArgumentException("判断条件缺少必要参数：" + condition);
        }

        // 1. 按 && 拆分
        String[] andBlocks = condition.split("&&");

        for (String block : andBlocks) {
            String exp = block.trim();
            if (exp.isEmpty()) continue;

            Boolean blockResult = (Boolean) AviatorEvaluator.execute(exp, env);
            if (!blockResult) {
                // 一旦某个 && 块为 false，整体即 false
                return false;
            }
        }

        return true;  // 所有块都满足
    }

    /**
     * 获取打破参数约束条件的参数值
     *
     * @param paramList
     * @param paramEnv
     * @return
     */
    public Map<ParamType, Double> getParamConstraintValue(List<String> paramList, Map<String, Object> paramEnv, Map<String, Object> conditionEnv) {
        if (paramList.isEmpty()) {
            throw new IllegalArgumentException("请检查 " + description + " 该约束的约束参数！");
        }
        if (paramEnv == null || paramEnv.isEmpty()) {
            throw new IllegalArgumentException("请检查 " + description + " 判断条件是否生效的输入！");
        }
        Map<ParamType, Double> result = new EnumMap<>(ParamType.class);
        for (String exp : paramList) {
            // 判断表达式是否满足
            boolean ok = (Boolean) AviatorEvaluator.execute(exp, paramEnv);
            if (!ok) { // 打破约束条件
                // 提取参数名（写法始终是 param op value）
                String paramName = DisplayUtils.getMessageFromExp(exp, "param");
                String valueStr = DisplayUtils.getMessageFromExp(exp, "value");
                try {
                    ParamType type = ParamType.valueOf(paramName);
                    result.put(type, DisplayUtils.parseValue(valueStr, conditionEnv));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("未知的参数名: " + paramName, e);
                }
            }
        }
        return result;
    }

    /**
     * 获取打破参数约束条件的参数边界
     *
     * @param paramList
     * @param conditionEnv
     * @param initialBound
     */
    public void getParamBoundPair(List<String> paramList, Map<String, Object> conditionEnv, Map<ParamType, BoundPair> initialBound) {
        if (paramList == null || paramList.isEmpty()) {
            throw new IllegalArgumentException("请检查 " + description + " 该约束的约束参数！");
        }
        for (String exp : paramList) {
            String name = DisplayUtils.getMessageFromExp(exp, "param");
            String op = DisplayUtils.getMessageFromExp(exp, "op");
            String vStr = DisplayUtils.getMessageFromExp(exp, "value");
            // 解析参数名和数值
            ParamType type = ParamType.valueOf(name);
            double v = DisplayUtils.parseValue(vStr, conditionEnv);
            BoundPair bp = initialBound.get(type);
            //兜底措施
            if (bp == null) {
                bp = new BoundPair(ParamBoundType.getMin(name), 0.0, ParamBoundType.getMax(name), Double.MAX_VALUE); // 给个合理初始
                initialBound.put(type, bp);
            }
            // 根据操作符更新边界
            switch (op) {
                case "<", "<=" -> bp.setMaxVal(Math.min(v, bp.getMaxVal()));
                case ">", ">=" -> bp.setMinVal(Math.max(v, bp.getMinVal()));
                case "==", "=" -> {
                    bp.setMinVal(v);
                    bp.setMaxVal(v);
                }
                default -> throw new IllegalArgumentException("不支持的操作符: " + op);
            }
        }
        // 处理径流上下界联立关系
        setQBoundPair(initialBound);
    }

    /**
     * 设置径流上下界联立关系
     *
     * @param initialBound
     */
    public void setQBoundPair(Map<ParamType, BoundPair> initialBound) {
        BoundPair Qo = initialBound.get(ParamType.Qo);
        BoundPair Qp = initialBound.get(ParamType.Qp);
        double min = Qp.getMinVal();
        double max = Qo.getMaxVal();
        Qp.setMinVal(Qo.getMinVal());
        Qp.setMaxVal(Math.min(Qp.getMaxVal(), max));
        Qo.setMinVal(Math.max(Qo.getMinVal(), min));
        initialBound.put(ParamType.Qo, Qo);
        initialBound.put(ParamType.Qp, Qp);
    }

    /**
     * 获取第一个被打破的约束条件
     *
     * @param constraints
     * @param conditionEnv
     * @param type
     * @param sign
     * @return
     */
    public static ConstraintData getFirstViolatedConstraint(List<ConstraintData> constraints, Map<String, Object> conditionEnv, ParamType type, String sign) {
        List<ConstraintData> violatedConstraint = new ArrayList<>();
        for (ConstraintData constraint : constraints) {
            // 判断条件是否生效
            boolean isActive = constraint.isConditionActive(constraint.getCondition(), conditionEnv);
            if (isActive) {
                List<String> param = constraint.getParam();
                for (String exp : param) {
                    String name = DisplayUtils.getMessageFromExp(exp, "param");
                    String op = DisplayUtils.getMessageFromExp(exp, "op");
                    if (name.equals(type.name())) {
                        if (sign.equals("下界放宽") && (op.equals(">") || op.equals(">="))) {
                            violatedConstraint.add(constraint);
                        }
                        if (sign.equals("上界放宽") && (op.equals("<") || op.equals("<="))) {
                            violatedConstraint.add(constraint);
                        }
                    }
                }
            }
        }
        if (violatedConstraint.isEmpty()) {
            return null; // 没有任何约束被打破
        } else {
            return violatedConstraint.stream().min(Comparator.comparing(v -> v.rigid)).get(); // 返回第一个被打破的软约束
        }
    }
}
