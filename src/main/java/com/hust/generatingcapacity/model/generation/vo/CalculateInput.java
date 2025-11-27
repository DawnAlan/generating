package com.hust.generatingcapacity.model.generation.vo;

import com.hust.generatingcapacity.tools.TimeUtils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Data
@Getter
@Setter
public class CalculateInput {
    //电站名称
    private String station;
    //开始时间
    private Date start;
    //尺度
    private String period;
    //入库径流
    private List<PreFlow> inFlows;
    //开始水位
    private double waterLevel;
    //开始尾水位
    private double tailLevel = -10086.0;

    //调度期和入库径流长度一致性检查
    public void checkForecast(Integer schedulingL) {
        this.inFlows = this.inFlows.stream()
                .filter(preFlow -> TimeUtils.dateCompare(start, preFlow.getTime(), this.period) || preFlow.getTime().after(start))
                .sorted(Comparator.comparing(PreFlow::getTime))
                .toList();
        int flowL = this.inFlows.size();
        if (flowL >= schedulingL) {
            this.inFlows = this.inFlows.subList(0, schedulingL);
        } else {
            int needL = schedulingL - flowL;
            PreFlow lastFlow = this.inFlows.get(flowL - 1);
            for (int i = 1; i <= needL; i++) {
                Date newTime = TimeUtils.addCalendar(lastFlow.getTime(), this.period, i);
                this.inFlows.add(new PreFlow(newTime, lastFlow.getInFlow()));
            }
        }
    }


}
