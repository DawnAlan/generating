package com.hust.generatingcapacity.model.generation.dispatch;

import com.hust.generatingcapacity.GeneratingCapacityApplication;
import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.iservice.IHydropowerStationService;
import com.hust.generatingcapacity.model.generation.domain.ConstraintData;
import com.hust.generatingcapacity.model.generation.type.ParamType;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest(classes = GeneratingCapacityApplication.class)
public class ConstraintTest {
    @Autowired
    private IHydropowerStationService hydropowerStationService;

    @Test
    void testConstraintCondition() {
        StationInfDTO dto = hydropowerStationService.get("瀑布沟");
        StationData stationData = hydropowerStationService.changeToStationData(dto);
        Map<String, Object> condition = new ConstraintEnvBuilder().conditionBuild(9, 843, 3, 86400, 7000);
        Map<String, Object> paramEnv = new ConstraintEnvBuilder().paramBuild(843, 1, 200, 8200, 0.9,0.0);
//        System.out.println("电站状态为：" + condition);
        System.out.println("电站时段末状态为：" + paramEnv);
        List<ConstraintData> constraints = stationData.getConstraints();
        for (ConstraintData constraint : constraints) {
//            System.out.println(constraint.getDescription() + " \n是否生效：" + constraint.isConditionActive(constraint.getCondition(), condition));
            if (constraint.isConditionActive(constraint.getCondition(), condition)) {
                List<String> paramList = constraint.getParam();
                Map<ParamType, Double> param = constraint.getParamConstraintValue(paramList, paramEnv,condition);
                if (param != null && !param.isEmpty()) {
                    System.out.println("该约束被违反："+constraint.getDescription() + " \n打破约束的参数值：" + param);
                }
            }
        }
//        System.out.println(dto);
    }
}
