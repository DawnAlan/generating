package com.hust.generatingcapacity.model.generation.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Data
@Getter
@Setter
public class CalculateStep implements Cloneable{
    //电站名称
    private String station;
    //是否为修正
    private boolean isRevise;
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
    //结束水头
    private double headAft;
    //发电流量
    private double qp;
    //出库流量
    private double qo;
    //计算结果（WM*h）
    private double calGen;
    //备注
    private String remark;


    public String toString(int i) {
        return station+"电站，第"+i+"个时段计算结果{" +
                ",日期=" + time +
                ", 是否修正过=" + isRevise +
                ", inFlow=" + inFlow +
                ", levelBef=" + levelBef +
                ", levelAft=" + levelAft +
                ", head=" + head +
                ", headAft=" + headAft +
                ", qp=" + qp +
                ", qo=" + qo +
                ", calGen=" + calGen +
                '}';
    }

    @Override
    public CalculateStep clone() {
        try {
            return (CalculateStep) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone CalculateStep", e);
        }
    }

    public CalculateStep() {
        this.isRevise = false;
    }

    /**
     * 初始计算时刻赋值
     * @param input
     */
    public CalculateStep(CalculateInput input) {
        this.station = input.getStation();
        this.isRevise = false;
        this.time = input.getStart();
        this.inFlow = input.getInFlows().get(0).getInFlow();
        this.levelBef = input.getWaterLevel();
        this.head = input.getWaterLevel() - input.getTailLevel();
    }
}
