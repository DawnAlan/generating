package com.hust.generatingcapacity.model.generation.domain;

import com.googlecode.aviator.AviatorEvaluator;
import com.hust.generatingcapacity.dto.ConstraintInfDTO;
import com.hust.generatingcapacity.model.generation.type.ConditionType;
import com.hust.generatingcapacity.model.generation.type.ConstraintType;
import com.hust.generatingcapacity.model.generation.type.ParamBoundType;
import com.hust.generatingcapacity.model.generation.type.ParamType;
import com.hust.generatingcapacity.model.generation.util.DisplayUtils;
import com.hust.generatingcapacity.model.generation.vo.BoundPair;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Data
@Getter
@Setter
public class ConstraintData {
    //约束类型
    private ConstraintType constraintType;
    //是否为硬约束
    private Boolean rigid;
    //约束描述
    private String description;
    //生效条件
    private String condition;
    //约束参数
    private List<String> param;

    public ConstraintData() {
    }

    public ConstraintData(ConstraintInfDTO constraintInfDTO) {
        this.constraintType = ConstraintType.fromCode(constraintInfDTO.getConstraintType());
        this.rigid = constraintInfDTO.getRigid();
        this.description = constraintInfDTO.getDescription();
        this.condition = constraintInfDTO.getCondition();
        this.param = constraintInfDTO.getParam();
    }

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

        try {
            List<Boolean>  list = new ArrayList<>();
            for (ConditionType conditionType : ConditionType.values()) {
                boolean ok = false;
                //首先按条件参数进行划分
                List<String> splitParams = splitParamBlocks(condition,conditionType.name());
                if (!splitParams.isEmpty()) {
                    for (String param : splitParams) {//同一种参数条件按照“||”来划分
                        if ((Boolean) AviatorEvaluator.execute(param, env)){
                            list.add(true);
                            ok = true;
                        }
                    }
                    if (!ok) {
                        list.add(false);
                    }
                }
            }
            return list.stream().allMatch(Boolean::booleanValue);
        }catch (Exception e){
            return false;
        }

    }

    public static List<String> splitParamBlocks(String condition, String paramName) {
        if (condition == null || condition.isBlank()) {
            return List.of();
        }

        // 匹配形如：T <= 5.0  /  H >= 1375  /  Qp == 0  这种
        Pattern simpleCondPattern = Pattern.compile(
                "\\s*(?<param>[A-Za-z][A-Za-z0-9_]*)\\s*(?<op>>=|<=|>|<|==)\\s*(?<value>[^&|]+)\\s*"
        );

        // 先按 && / || 切成  term0 op0 term1 op1 term2 ...
        List<String> terms = new ArrayList<>();
        List<String> ops = new ArrayList<>();

        Matcher m = Pattern.compile("&&|\\|\\|").matcher(condition);
        int last = 0;
        while (m.find()) {
            String term = condition.substring(last, m.start()).trim();
            if (!term.isEmpty()) {
                terms.add(term);
            }
            ops.add(m.group()); // "&&" 或 "||"
            last = m.end();
        }
        String lastTerm = condition.substring(last).trim();
        if (!lastTerm.isEmpty()) {
            terms.add(lastTerm);
        }

        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int prevParamIndex = -1; // 上一个属于 paramName 的 term 的下标

        for (int i = 0; i < terms.size(); i++) {
            String term = terms.get(i).trim();
            if (term.isEmpty()) continue;

            Matcher tm = simpleCondPattern.matcher(term);
            if (!tm.matches()) {
                // 解析不出的条件，直接忽略（也可以选择丢到某个块里，看你需求）
                continue;
            }
            String param = tm.group("param");
            if (!paramName.equals(param)) {
                // 不是目标参数的条件，跳过
                continue;
            }

            if (current.isEmpty()) {
                // 第一块的第一个条件
                current.append(term);
            } else {
                // 找到前一个同参数条件和当前条件之间的连接符
                // term[i] 与 term[i-1] 之间的操作符是 ops[i-1]
                String opBetween = ops.get(prevParamIndex); // 注意：prevParamIndex 是上一个 term 的 index

                if ("||".equals(opBetween)) {
                    // 新建一个块
                    blocks.add(current.toString());
                    current.setLength(0);
                    current.append(term);
                } else { // "&&"
                    current.append(" && ").append(term);
                }
            }
            prevParamIndex = i;
        }

        if (current.length() > 0) {
            blocks.add(current.toString());
        }

        return blocks;
    }
    public static String normalizeCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            return condition;
        }

        // 简单原子条件：T <= 5.0  /  H >= 1375  这种
        Pattern simpleCondPattern = Pattern.compile(
                "^(?<param>[A-Za-z][A-Za-z0-9_]*)\\s*(?<op>>=|<=|>|<|==)\\s*(?<value>.+)$"
        );

        // 先按 "||" 拆 OR 子句
        String[] orClauses = condition.split("\\|\\|");
        List<String> normalizedClauses = new ArrayList<>();

        for (String rawClause : orClauses) {
            String clause = rawClause.trim();
            if (clause.isEmpty()) continue;

            // 子句内部按 "&&" 拆成原子条件
            String[] terms = clause.split("&&");

            // param -> 该参数相关的条件列表
            Map<String, List<String>> byParam = new LinkedHashMap<>();
            // 解析不出来的复杂条件，先原样保留
            List<String> others = new ArrayList<>();

            for (String rawTerm : terms) {
                String term = rawTerm.trim();
                if (term.isEmpty()) continue;

                Matcher m = simpleCondPattern.matcher(term);
                if (m.matches()) {
                    String param = m.group("param");
                    byParam.computeIfAbsent(param, k -> new ArrayList<>())
                            .add(term);
                } else {
                    // 比如包含多参数、函数调用之类，先丢到 others，避免丢表达式
                    others.add(term);
                }
            }

            // 组装一个子句：同一参数的条件用 && 聚在一起
            List<String> groupedParts = new ArrayList<>();

            for (Map.Entry<String, List<String>> e : byParam.entrySet()) {
                List<String> conds = e.getValue();
                // 这里可以根据 op 排序（比如 >= 再 <=），现在先保持原顺序
                String joined = conds.stream().collect(Collectors.joining(" && "));
                groupedParts.add(joined);
            }

            // 把 others 也并回去
            groupedParts.addAll(others);

            if (groupedParts.isEmpty()) {
                continue;
            }

            String joinedClause = String.join(" && ", groupedParts);
            normalizedClauses.add("(" + joinedClause + ")");
        }

        if (normalizedClauses.isEmpty()) {
            return condition.trim();
        }

        return String.join(" || ", normalizedClauses);
    }

    /**
     * 获取硬约束中所有的参数数量
     *
     * @param env
     * @param constraints
     * @return
     */
    public int numParamOfRigid(Map<String, Object> env, List<ConstraintData> constraints) {
        int result = 0;
        for (ConstraintData constraintData : constraints) {
            if (constraintData.isConditionActive(constraintData.getCondition(), env) && constraintData.getRigid()) {
                List<String> params = constraintData.getParam();
                Set<String> paramSet = new HashSet<>();
                for (String param : params) {
                    paramSet.add(DisplayUtils.getMessageFromExp(param, "param"));
                }
                result = result + paramSet.size();
            }
        }
        return result;
    }

    /**
     * 获取软约束中所有的参数数量
     *
     * @param env
     * @param constraints
     * @return
     */
    public int numParamOfSoft(Map<String, Object> env, List<ConstraintData> constraints) {
        int result = 0;
        for (ConstraintData constraintData : constraints) {
            if (constraintData.isConditionActive(constraintData.getCondition(), env) && !constraintData.getRigid()) {
                List<String> params = constraintData.getParam();
                Set<String> paramSet = new HashSet<>();
                for (String param : params) {
                    paramSet.add(DisplayUtils.getMessageFromExp(param, "param"));
                }
                result = result + paramSet.size();
            }
        }
        return result;
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
