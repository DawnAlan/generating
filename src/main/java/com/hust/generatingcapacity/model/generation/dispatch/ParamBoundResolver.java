package com.hust.generatingcapacity.model.generation.dispatch;

import com.hust.generatingcapacity.model.generation.domain.ConstraintData;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.model.generation.vo.CalculateParam;
import com.hust.generatingcapacity.model.generation.vo.CalculateStep;
import com.hust.generatingcapacity.model.generation.vo.ParamValue;
import com.hust.generatingcapacity.tools.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParamBoundResolver {




    private static List<ParamValue> getParamMap(CalculateStep data, CalculateStep data_aft, CalculateParam calParam, StationData stationData) {
        //约束条件
        Integer T = TimeUtils.getSpecificDate(data.getTime()).get("月");
        double H = data.getLevelBef();
        double Qin = data.getInFlow();
        Map<String, Object> conditionEnv = new ConstraintEnvBuilder().conditionBuild(T, H, calParam.getSchedulingL(), calParam.getPeriod(), Qin);
        //检查约束
        List<ParamValue> result = new ArrayList<>();
        List<ConstraintData> constraints = stationData.getConstraints();
        for (ConstraintData constraint : constraints) {
            if (constraint.isConditionActive(constraint.getCondition(), conditionEnv)) {//约束生效
                List<String> paramList = constraint.getParam();
//                Map<ParamType, Double> param = constraint.getParamConstraintValue(paramList, paramEnv, conditionEnv);
//                if (param != null && !param.isEmpty()) {
//                    for (Map.Entry<ParamType, Double> entry : param.entrySet()) {
//                        ParamValue pv = new ParamValue(entry.getKey(), entry.getValue(), constraint.getDescription(), constraint.isRigid());
//                        result.add(pv);
//                    }
//                }
            }
        }
        return result;
    }
}
