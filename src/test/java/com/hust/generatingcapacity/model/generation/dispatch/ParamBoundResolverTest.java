package com.hust.generatingcapacity.model.generation.dispatch;

import com.hust.generatingcapacity.GeneratingCapacityApplication;
import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.iservice.IHydropowerStationService;
import com.hust.generatingcapacity.model.generation.domain.ConstraintData;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.model.generation.type.ParamType;
import com.hust.generatingcapacity.model.generation.vo.BoundPair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest(classes = GeneratingCapacityApplication.class)
public class ParamBoundResolverTest {
    @Autowired
    private IHydropowerStationService hydropowerStationService;

    @Test
    void testParamBoundResolver() {
        StationInfDTO dto = hydropowerStationService.get("瀑布沟");
        StationData stationData = hydropowerStationService.changeToStationData(dto);
        Map<String, Object> condition = new ConstraintEnvBuilder().conditionBuild(9, 843, 3, 86400, 7000);
//        System.out.println("电站状态为：" + condition);
        List<ConstraintData> constraints = stationData.getConstraints();
        Map<ParamType, BoundPair> cound = stationData.setInitialBoundPair();
        for (ConstraintData constraint : constraints) {
            if (constraint.isConditionActive(constraint.getCondition(), condition)) {
                List<String> paramList = constraint.getParam();
                new ConstraintData().getParamBoundPair(paramList, condition, cound);
            }
        }
        if (cound != null && !cound.isEmpty()) {
            System.out.println("参数边界为：" + cound.values());
        }
//        System.out.println(dto);
    }
}
