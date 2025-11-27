package com.hust.generatingcapacity.model.generation.vo;

import com.hust.generatingcapacity.model.generation.domain.StationData;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CalculateVO {
    //每一步的计算结果
    private CalculateStep calStep;
    //输入的径流、初水位等运行信息
    private CalculateInput calInput;
    //调度方式等信息
    private CalculateParam calParam;
    //预设条件
    private CalculateCondition calCondition;
    //电站相关数据
    private StationData stationData;

    public CalculateVO() {
    }

    public CalculateVO(CalculateStep calStep, CalculateInput calInput, CalculateParam calParam, StationData stationData) {
        this.calStep = calStep;
        this.calInput = calInput;
        this.calParam = calParam;
        this.stationData = stationData;
    }

    public CalculateVO(CalculateStep calStep, CalculateParam calParam, StationData stationData) {
        this.calStep = calStep;
        this.calParam = calParam;
        this.stationData = stationData;
    }

    public CalculateVO(CalculateStep calStep, CalculateParam calParam) {
        this.calStep = calStep;
        this.calParam = calParam;
    }
}
