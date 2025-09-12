package com.hust.generatingcapacity.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Entity
@Table(name = "ddh_hystation_tailwaterlevel")
@Comment("尾水位流量关系曲线")
public class TailWaterLevelFlow {//尾水位流量关系曲线
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "water_level", columnDefinition = "DOUBLE COMMENT '尾水位（m）'")
    private Double waterLevel;
    @Column(name = "out_flow", columnDefinition = "DOUBLE COMMENT '出库流量（m³/s）'")
    private Double outFlow;
    // 关联父表，水电站信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ddh_hystation_id")
    @JsonBackReference
    private HydropowerStation hydropowerStation;

}
