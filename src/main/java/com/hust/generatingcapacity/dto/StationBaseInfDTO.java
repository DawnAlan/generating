package com.hust.generatingcapacity.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class StationBaseInfDTO {
    //电站名称
    private String stationName;
    //流域
    private String basin;
    //建设状态（1-投产、2-在建、3-拟建）
    private Integer status;
    //装机容量（MW）
    private Double installedCapacity;
    //上游电站
    private String upperStation;
    //下游电站
    private String lowerStation;
    //经度
    private Double longitude;
    //纬度
    private Double latitude;
    //是否采用规程计算
    private Boolean isProceduralCalculation;
    //是否参与四川电力市场
    private Boolean isParticipateMarket;
    //是否有包含水位库容曲线
    private Boolean isReservoirInf;
    //是否有水位耗水率曲线
    private Boolean isWaterConsumptionLine;
    //是否有尾水位流量曲线(流量为code，水位为value)
    private Boolean isTailLevelFlowLine;
    //是否有机组信息
    private Boolean isUnitInfs;
    //是否有约束信息
    private Boolean isConstraintInfs;

    public StationBaseInfDTO() {
    }

    public StationBaseInfDTO(StationInfDTO dto) {
        this.stationName = dto.getStationName();
        this.basin = dto.getBasin();
        this.status = dto.getStatus();
        this.installedCapacity = dto.getInstalledCapacity();
        this.upperStation = dto.getUpperStation();
        this.lowerStation = dto.getLowerStation();
        this.isProceduralCalculation = dto.getIsProceduralCalculation();
        this.isParticipateMarket = dto.getIsParticipateMarket();
        this.longitude = dto.getLongitude();
        this.latitude = dto.getLatitude();
    }

    public StationBaseInfDTO(String stationName, String basin, Integer status, Double installedCapacity, String upperStation, String lowerStation, Boolean isProceduralCalculation, Boolean isParticipateMarket) {
        this.stationName = stationName;
        this.basin = basin;
        this.status = status;
        this.installedCapacity = installedCapacity;
        this.upperStation = upperStation;
        this.lowerStation = lowerStation;
        this.isProceduralCalculation = isProceduralCalculation;
        this.isParticipateMarket = isParticipateMarket;
    }
}
