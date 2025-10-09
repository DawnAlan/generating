package com.hust.generatingcapacity.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Entity
@Table(name = "ddh_hystation_waterconsumption")
@Comment("水位耗水率曲线表")
public class WaterLevelConsumption {// 水位耗水率曲线
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "water_level", columnDefinition = "DOUBLE COMMENT '水位（m）'")
    private Double waterLevel;
    @Column(name = "consumption", columnDefinition = "DOUBLE COMMENT '耗水率（m³/kWh）'")
    private Double consumption;
    @Column(name = "is_water_level", columnDefinition = "bit(1) COMMENT '是否为水位'")
    private Boolean isWaterLevel;
    // 关联父表，水电站信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ddh_hystation_id")
    @JsonBackReference
    private HydropowerStation hydropowerStation;
}
