package com.hust.generatingcapacity.dto;

import com.hust.generatingcapacity.tools.StreamUtils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.hust.generatingcapacity.model.generation.util.DisplayUtils.listToStringWithLimit;

@Data
@Getter
@Setter
public class StationInfDTO {
    //电站名称
    private String stationName;
    //电站代码
    private Double stationCode;
    //流域
    private String basin;
    //经度
    private Double longitude;
    //纬度
    private Double latitude;
    //建设状态（1-投产、2-在建、3-拟建）
    private Integer status;
    //装机容量（MW）
    private Double installedCapacity;
    //调节性能(无、日调节、季调节、年调节)
    private String regulationPerformance;
    //输电断面
    private String transmissionSection;
    //上游电站
    private String upperStation;
    //下游电站
    private String lowerStation;
    //是否归大渡河公司管辖
    private Boolean isUnderDdh;
    //是否留川
    private Boolean isStaySichuan;
    //是否参与电力市场
    private Boolean isParticipateMarket;
    //水库信息包含水位库容曲线
    private ReservoirInfDTO reservoirInf;
    //是否为水位耗水率（否——水头耗水率曲线）
    private Boolean isWaterConsumption;
    //水位耗水率曲线
    private List<CodeValueDTO> waterConsumptionLine;
    //尾水位流量曲线(流量为code，水位为value)
    private List<CodeValueDTO> tailLevelFlowLine;
    //机组信息
    private List<UnitInfDTO> unitInfs;
    //约束信息
    private List<ConstraintInfDTO> constraintInfs;

    public StationInfDTO(String stationName, Double stationCode, String basin, Double longitude, Double latitude, Integer status, Double installedCapacity, String regulationPerformance,
                         String transmissionSection, String upperStation, String lowerStation, Boolean isUnderDdh, Boolean isStaySichuan, Boolean isParticipateMarket) {
        this.stationName = stationName;
        this.stationCode = stationCode;
        this.basin = basin;
        this.longitude = longitude;
        this.latitude = latitude;
        this.status = status;
        this.installedCapacity = installedCapacity;
        this.regulationPerformance = regulationPerformance;
        this.transmissionSection = transmissionSection;
        this.upperStation = upperStation;
        this.lowerStation = lowerStation;
        this.isUnderDdh = isUnderDdh;
        this.isStaySichuan = isStaySichuan;
        this.isParticipateMarket = isParticipateMarket;
    }

    public StationInfDTO() {
    }

    private String statusToString() {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "投产";
            case 2 -> "在建";
            case 3 -> "拟建";
            default -> "未知";
        };
    }

    @Override
    public String toString() {
        String unitStr = (unitInfs == null || unitInfs.isEmpty()) ? "[ 无 ]" :
                unitInfs.stream()
                        .filter(Objects::nonNull)
                        .filter(StreamUtils.distinctByKey(UnitInfDTO::getUnitName)) // ✅ 去重
                        .map(UnitInfDTO::toString)
                        .collect(Collectors.joining("\n    ", "[\n    ", "\n]"));
        String constraintStr = (constraintInfs == null || constraintInfs.isEmpty()) ? "[ 无 ]" :
                constraintInfs.stream()
                        .filter(Objects::nonNull)
                        .limit(10) // 最多显示10条
                        .map(ConstraintInfDTO::toString)
                        .collect(Collectors.collectingAndThen(
                                Collectors.joining("\n    ", "[\n    ", "\n]"),
                                str -> {
                                    int total = (int) constraintInfs.stream().filter(Objects::nonNull).count();
                                    if (total > 10) {
                                        return str.substring(0, str.length() - 1) // 去掉最后一个 ]
                                                + "    ...还有 " + (total - 10) + " 条数据未显示\n]";
                                    }
                                    return str;
                                }
                        ));
        String nhqStr = (unitInfs == null || unitInfs.isEmpty()) ? "[ 无 ]" :
                unitInfs.stream()
                        .filter(Objects::nonNull)
                        .filter(StreamUtils.distinctByKey(UnitInfDTO::getUnitName)) // ✅ 去重
                        .map(u -> listToStringWithLimit(u.getNhqLine(), 10))
                        .collect(Collectors.joining("\n    ", "[\n    ", "\n]"));

        return "电站基础信息汇总：{" +
                "电站名称='" + stationName + '\'' +
                ", 流域='" + basin + '\'' +
                ", 建设状态=" + statusToString() +
                ", 装机=" + installedCapacity +
                ", 调节性能='" + regulationPerformance + '\'' +
                ", 输电断面='" + transmissionSection + '\'' +
                ", 上游电站='" + upperStation + '\'' +
                ", 下游电站='" + lowerStation + '\'' +
                ", 是否大渡河公司管辖=" + isUnderDdh + '\'' + ",\n" +
                "水库特征信息：" + reservoirInf + '\'' + ",\n" +
                "机组信息：" + unitStr +
                ",约束信息：" + constraintStr +
                ",水位耗水率曲线：" + listToStringWithLimit(waterConsumptionLine, 10) +
                ",尾水位流量曲线：" + listToStringWithLimit(tailLevelFlowLine, 10) +
                ",NHQ曲线：" + nhqStr +
                '}';
    }
}
