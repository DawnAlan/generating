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
    //预见期
    private Integer L;
    //入库径流
    private List<PreFlow> inFlows;
    //开始水位
    private Double waterLevel;
    //开始尾水位
    private Double tailLevel;
    //预见期结束水位
    private Double finalLevel;

    //预见期和入库径流长度一致性检查
    public void checkForecast() {
        int flowL = this.inFlows.size();
        if (flowL >= L) {
            this.inFlows = this.inFlows.stream()
                    .filter(preFlow -> TimeUtils.dateCompare(start, preFlow.getTime(), this.period) || preFlow.getTime().after(start))
                    .sorted(Comparator.comparing(PreFlow::getTime))
                    .toList();
            if (this.inFlows.size() >= L) {
                this.inFlows = this.inFlows.subList(0, L);
            } else {
                this.L = this.inFlows.size();
                System.out.println(station + " 水电站预见期长度大于入库径流预报长度，已调整预见期长度为 " + this.L);
            }
        } else {
            this.L = this.inFlows.size();
            System.out.println(station + " 水电站预见期长度大于入库径流预报长度，已调整预见期长度为 " + this.L);
        }
        if (this.L <= 0) {
            throw new IllegalArgumentException(station + " 水电站预见期长度为0，请检查入库径流数据！");
        }
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
