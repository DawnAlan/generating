package com.hust.generatingcapacity.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hust.generatingcapacity.model.generation.type.DispatchType;
import com.hust.generatingcapacity.model.generation.vo.CalculateStep;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.util.Date;

@Data
@Entity
@Table(name = "ddh_hystation_generation_cal_station_out")
@Comment("单个时段电站发电能力计算结果")
public class GenerationCalStationOut {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "gen_status", columnDefinition = "INT COMMENT '0-最小，1-最大，2-手动'")
    private Integer genStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "time", columnDefinition = "datetime COMMENT '计算时间'")
    private Date time;
    @Column(name = "station", columnDefinition = "TEXT COMMENT '水电站'")
    private String station;
    @Column(name = "in_flow", columnDefinition = "DOUBLE COMMENT '入库径流'")
    private Double inFlow;
    @Column(name = "level_before", columnDefinition = "DOUBLE COMMENT '开始水位'")
    private Double levelBef;
    @Column(name = "level_after", columnDefinition = "DOUBLE COMMENT '结束水位'")
    private Double levelAft;
    @Column(name = "q_power", columnDefinition = "DOUBLE COMMENT '发电流量'")
    private Double qp;
    @Column(name = "q_out", columnDefinition = "DOUBLE COMMENT '出库流量'")
    private Double qo;
    @Column(name = "cal_generation", columnDefinition = "DOUBLE COMMENT '发电计算结果'")
    private Double calGen;
    @Column(name = "cal_history", columnDefinition = "DOUBLE COMMENT '历史真实发电量'")
    private Double calHis;
    @Column(name = "remark", columnDefinition = "TEXT COMMENT '备注'")
    private String remark;

    // 关联父表，流域计算结果
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ddh_hystation_generation_cal_basin_out_id")
    @JsonBackReference
    private GenerationCalBasinOut generationCalBasinOut;

    public GenerationCalStationOut() {
    }

    public GenerationCalStationOut(CalculateStep step, boolean isGenMin, DispatchType dispatchType) {
        if (dispatchType == DispatchType.PRE_CONDITION) {
            this.genStatus = 2;
        }else {
            this.genStatus = isGenMin ? 0 : 1;
        }
        this.time = step.getTime();
        this.station = step.getStation();
        this.inFlow = step.getInFlow();
        this.levelBef = step.getLevelBef();
        this.levelAft = step.getLevelAft();
        this.qp = step.getQp();
        this.qo = step.getQo();
        this.calGen = step.getCalGen();
        this.remark = step.getRemark();
    }
}
