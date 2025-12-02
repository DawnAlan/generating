package com.hust.generatingcapacity.model.generation.domain;

import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.model.generation.type.ParamBoundType;
import com.hust.generatingcapacity.model.generation.type.ParamType;
import com.hust.generatingcapacity.model.generation.vo.BoundPair;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
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
    // 输电断面容量
    private Double transmissionCapacity = Double.MAX_VALUE;
    //  是否归大渡河公司管辖
    private Boolean isUnderDdh;
    //  是否采用规程计算
    private Boolean isProceduralCalculation;
    //  是否参与电力市场
    private Boolean isParticipateMarket;
    //  校核洪水位（米）
    private Double checkFloodLevel = Double.MAX_VALUE;
    //  最小库水位（米）
    private Double minRegulateLevel = 0.0;
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
        this.isProceduralCalculation = stationInfDTO.getIsProceduralCalculation();
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
                ", isProceduralCalculation=" + isProceduralCalculation +
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


    /**
     * 根据水电站信息设置初始边界
     * @return
     */
    public Map<ParamType, BoundPair> setInitialBoundPair() {
        Map<ParamType, BoundPair> initialBound = new EnumMap<>(ParamType.class);
        initialBound.put(ParamType.Qp, new BoundPair(ParamBoundType.Qp_MIN, 0.0, ParamBoundType.Qp_MAX, Double.MAX_VALUE));
        initialBound.put(ParamType.Qo, new BoundPair(ParamBoundType.Qo_MIN, 0.0, ParamBoundType.Qo_MAX, Double.MAX_VALUE));
        initialBound.put(ParamType.H, new BoundPair(ParamBoundType.H_MIN, this.getMinRegulateLevel(), ParamBoundType.H_MAX, this.getCheckFloodLevel()));
        initialBound.put(ParamType.dH, new BoundPair(ParamBoundType.dH_MIN, -Double.MAX_VALUE, ParamBoundType.dH_MAX, Double.MAX_VALUE));
        initialBound.put(ParamType.C, new BoundPair(ParamBoundType.C_MIN, 0.0, ParamBoundType.C_MAX, this.getTransmissionCapacity()));
        initialBound.put(ParamType.P, new BoundPair(ParamBoundType.P_MIN, 0.0, ParamBoundType.P_MAX, Math.min(this.getInstalledCapacity(),this.getTransmissionCapacity())));
        return initialBound;
    }

    public String getLowerStation() {
        return lowerStation == null ? "" : lowerStation;
    }

    public List<CodeValue> getWaterConsumptionLine() {
        return waterConsumptionLine == null ? Collections.emptyList() : waterConsumptionLine;
    }

    public List<CodeValue> getReservoirStorageLine() {
        return reservoirStorageLine == null ? Collections.emptyList() : reservoirStorageLine;
    }

    public List<CodeValue> getTailLevelFlowLine() {
        return tailLevelFlowLine == null ? Collections.emptyList() : tailLevelFlowLine;
    }

    public List<ConstraintData> getConstraints() {
        return constraints == null ? Collections.emptyList() : constraints;
    }

    public List<List<NHQData>> getNHQLines() {
        return NHQLines == null ? Collections.emptyList() : NHQLines;
    }




}
