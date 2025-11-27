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
    //尺度
    private Integer period;
    //调度期长度
    private Integer schedulingL;
    //是否为区间径流
    private boolean isIntervalFlow;
    //发电上下限
    private boolean isGenMin;
    //是否考虑水头变化对耗水率的影响
    private boolean isConsiderH;
    //调度方式
    private DispatchType dispatchType;

    public CalculateParam(String station, Integer period, Integer schedulingL, boolean isIntervalFlow, boolean isGenMin, boolean isConsiderH, DispatchType dispatchType) {
        this.station = station;
        this.period = period;
        this.schedulingL = schedulingL;
        this.isIntervalFlow = isIntervalFlow;
        this.isGenMin = isGenMin;
        this.isConsiderH = isConsiderH;
        this.dispatchType = dispatchType;
    }

    public CalculateParam() {
        this.dispatchType = DispatchType.RULE_BASED;
    }

    //尺度转换为秒
    public static Integer changePeriod(String period) {
        return switch (period) {
            case "小时" -> 3600;
            case "日" -> 86400;
            case "月" -> 2678400;
            case "年" -> 31536000;
            default -> throw new IllegalArgumentException("请检查时间尺度！");
        };
    }
}
