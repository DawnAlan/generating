package com.hust.generatingcapacity.model.generation.vo;

import com.hust.generatingcapacity.model.generation.type.DispatchType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CalculateParam {
    private String station;
    private Integer period;
    private Integer L;
    //是否为区间径流
    private boolean isIntervalFlow;
    //发电上下限
    private boolean isGenMin;
    //是否考虑水头
    private boolean isConsiderH;
    //调度方式
    private DispatchType dispatchType;

    public CalculateParam(String station, Integer period, Integer l, boolean isIntervalFlow, boolean isGenMin, boolean isConsiderH, DispatchType dispatchType) {
        this.station = station;
        this.period = period;
        this.L = l;
        this.isIntervalFlow = isIntervalFlow;
        this.isGenMin = isGenMin;
        this.isConsiderH = isConsiderH;
        this.dispatchType = dispatchType;
    }

    public CalculateParam() {
        this.dispatchType = DispatchType.RULE_BASED;
    }
}
