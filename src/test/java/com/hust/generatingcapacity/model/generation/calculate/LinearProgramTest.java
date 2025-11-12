package com.hust.generatingcapacity.model.generation.calculate;

import com.hust.generatingcapacity.GeneratingCapacityApplication;
import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.iservice.IHydropowerStationService;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.model.generation.vo.CalculateParam;
import com.hust.generatingcapacity.model.generation.vo.CalculateStep;

import com.hust.generatingcapacity.tools.TimeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;



@SpringBootTest(classes = GeneratingCapacityApplication.class)
public class LinearProgramTest {
    @Autowired
    private IHydropowerStationService hydropowerStationService;

    @Test
    void testSimplex() {
        // 入库径流
        double Qin = 762.0;
        // 时段初水位
        double Hb = 1829.16;
        double Tailb = 1701.36;
        // 时间间隔，单位秒
        int t = 86400;
        //获取参数边界
        StationInfDTO dto = hydropowerStationService.get("猴子岩");
        StationData stationData = hydropowerStationService.changeToStationData(dto);
        CalculateStep data = new CalculateStep();
        data.setStation("猴子岩");
        data.setInFlow(Qin);
        data.setRevise(false);
        data.setLevelBef(Hb);
        data.setTime(TimeUtils.createDate(2020,11,9,0,0));
        CalculateParam calParam = new CalculateParam();
        calParam.setPeriod(t);
        calParam.setGenMin(false);
        calParam.setConsiderH(true);
        calParam.setStation("猴子岩");
        calParam.setL(1);
        data = RuleOptimalCal.run(data,calParam,stationData);
        Assertions.assertNotNull(data);
        System.out.println(data.toString(1));

    }



}
