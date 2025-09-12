package com.hust.generatingcapacity.model.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Data
@Getter
@Setter
public class HisStationData {

    private String stationName;

    private Date time;

    private double inFlow;

    private double outFlow;

    private double waterLevelUp;

    private double waterLevelDown;

    private double powerOutput;   // 出力

    private double generation;    // 发电量

}
