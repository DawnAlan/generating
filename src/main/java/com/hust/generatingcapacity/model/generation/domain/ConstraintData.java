package com.hust.generatingcapacity.model.generation.domain;

import com.googlecode.aviator.AviatorEvaluator;
import com.hust.generatingcapacity.model.generation.type.ParamType;
import com.hust.generatingcapacity.model.generation.util.DisplayUtils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;


@Data
@Getter
@Setter
public class ConstraintData {
    //约束类型
    private String constraintType;
    //是否为硬约束
    private boolean isRigid;
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
            throw new IllegalArgumentException("请检查 " + description + " 判断条件是否生效的输入！");
        }
//        System.out.println(condition);
        return (Boolean) AviatorEvaluator.execute(condition, env);
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


}
