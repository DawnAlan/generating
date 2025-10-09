package com.hust.generatingcapacity.model.generation.vo;

import com.hust.generatingcapacity.model.generation.domain.StationData;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CalculateVO {
    private CalculateStep calStep;
    private CalculateInput calInput;
    private CalculateParam calParam;
    private StationData stationData;

    public CalculateVO() {
    }

    public CalculateVO(CalculateStep calStep, CalculateInput calInput, CalculateParam calParam, StationData stationData) {
        this.calStep = calStep;
        this.calInput = calInput;
        this.calParam = calParam;
        this.stationData = stationData;
    }

    public CalculateVO(CalculateStep calStep, CalculateInput calInput, CalculateParam calParam) {
        this.calStep = calStep;
        this.calInput = calInput;
        this.calParam = calParam;
    }

    public CalculateVO(CalculateStep calStep, CalculateParam calParam) {
        this.calStep = calStep;
        this.calParam = calParam;
    }
}
