package com.hust.generatingcapacity.model.generation.domain;

import com.hust.generatingcapacity.dto.StationInfDTO;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.hust.generatingcapacity.model.generation.util.DisplayUtils.listToStringWithLimit;

@Data
@Setter
@Getter
public class StationData {
    // 电站名称
    private String stationName;
    // 流域
    private String basin;
    // 是否投产
    private boolean status;
    // 调节性能
    private String regulation;
    // 装机容量
    private double installedCapacity;
    // 上游电站
    private String upperStation;
    // 下游电站
    private String lowerStation;
    // 输电断面
    private String transmissionSection;
    //  是否归大渡河公司管辖
    private Boolean isUnderDdh;
    //  是否留川
    private Boolean isStaySichuan;
    //  是否参与电力市场
    private Boolean isParticipateMarket;
    //  校核洪水位（米）
    private Double checkFloodLevel;
    //  最小库水位（米）
    private Double minRegulateLevel;
    //  约束信息
    private List<ConstraintData> constraints;
    //  水位库容曲线
    private List<CodeValue> reservoirStorageLine;
    //  水位耗水率曲线
    private List<CodeValue> waterConsumptionLine;
    //  是否为水位耗水率
    private Boolean isWaterConsumption;
    //  尾水位流量曲线(流量为code，水位为value)
    private List<CodeValue> tailLevelFlowLine;
    //  NHQ曲线
    private List<List<NHQData>> NHQLines;

    //除特征曲线外的其他信息
    public StationData(StationInfDTO stationInfDTO) {
        this.stationName = stationInfDTO.getStationName();
        this.basin = stationInfDTO.getBasin();
        this.status = stationInfDTO.getStatus() != null && stationInfDTO.getStatus() == 1;
        this.regulation = stationInfDTO.getRegulationPerformance();
        this.installedCapacity = stationInfDTO.getInstalledCapacity() != null ? stationInfDTO.getInstalledCapacity() : 0.0;
        this.upperStation = stationInfDTO.getUpperStation();
        this.lowerStation = stationInfDTO.getLowerStation();
        this.transmissionSection = stationInfDTO.getTransmissionSection();
        this.isUnderDdh = stationInfDTO.getIsUnderDdh();
        this.isStaySichuan = stationInfDTO.getIsStaySichuan();
        this.isParticipateMarket = stationInfDTO.getIsParticipateMarket();
        if (stationInfDTO.getReservoirInf() != null) {
            this.checkFloodLevel = stationInfDTO.getReservoirInf().getCheckFloodLevel();
            this.minRegulateLevel = stationInfDTO.getReservoirInf().getMinRegulateLevel();
        }
    }

    public StationData() {
    }

    @Override
    public String toString() {
        String nhqStr = (NHQLines == null || NHQLines.isEmpty()) ? "[ 无 ]" :
                NHQLines.stream()
                        .filter(Objects::nonNull)
                        .map(u -> listToStringWithLimit(u, 10))
                        .collect(Collectors.joining("\n    ", "[\n    ", "\n]"));
        return "StationData{" +
                "stationName='" + stationName + '\'' +
                ", basin='" + basin + '\'' +
                ", status=" + status +
                ", regulation='" + regulation + '\'' +
                ", installedCapacity=" + installedCapacity +
                ", upperStation='" + upperStation + '\'' +
                ", lowerStation='" + lowerStation + '\'' +
                ", transmissionSection='" + transmissionSection + '\'' +
                ", isUnderDdh=" + isUnderDdh +
                ", isStaySichuan=" + isStaySichuan +
                ", isParticipateMarket=" + isParticipateMarket +
                ", checkFloodLevel=" + checkFloodLevel +
                ", minRegulateLevel=" + minRegulateLevel + '\n' +
                ", constraints=" + listToStringWithLimit(constraints, 10) + '\n' +
                ", reservoirStorageLine=" + listToStringWithLimit(reservoirStorageLine, 10) + '\n' +
                ", waterConsumptionLine=" + listToStringWithLimit(waterConsumptionLine, 10) + '\n' +
                ", tailLevelFlowLine=" + listToStringWithLimit(tailLevelFlowLine, 10) + '\n' +
                ", NHQLines=" + nhqStr +
                '}';
    }
}
