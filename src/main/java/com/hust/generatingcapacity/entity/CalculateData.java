package com.hust.generatingcapacity.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Data
@Getter
@Setter
public class CalculateData {
    //电站名称
    private String station;
    //时间
    private Date time;
    //入库径流
    private double inFlow;
    //开始水位
    private double levelBef;
    //结束水位
    private double levelAft;
    //水头
    private double head;
    //机组数量
    private int units;
    //计算结果（WM*h）
    private double calGen;
}
