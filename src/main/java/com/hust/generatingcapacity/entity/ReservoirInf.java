package com.hust.generatingcapacity.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.util.List;

@Data
@Entity
@Table(name = "ddh_hystation_reservoir")
@Comment("水库特征信息")
public class ReservoirInf {// 水库特征信息
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "reservoir_name", columnDefinition = "TEXT COMMENT '水库名称'")
    private String reservoirName;
    @Column(name = "total_capacity", columnDefinition = "DOUBLE COMMENT '水库总库容（百万立方米）'")
    private Double totalCapacity;
    @Column(name = "effective_capacity", columnDefinition = "DOUBLE COMMENT '水库调节库容（百万立方米）'")
    private Double effectiveCapacity;
    @Column(name = "dead_capacity", columnDefinition = "DOUBLE COMMENT '水库死库容（百万立方米）'")
    private Double deadCapacity;
    @Column(name = "normal_waterLevel", columnDefinition = "DOUBLE COMMENT '正常蓄水位（米）'")
    private Double normalWaterLevel;
    @Column(name = "check_floodLevel", columnDefinition = "DOUBLE COMMENT '校核洪水位（米）'")
    private Double checkFloodLevel;
    @Column(name = "min_regulateLevel", columnDefinition = "DOUBLE COMMENT '最小库水位（米）'")
    private Double minRegulateLevel;
    // 关联父表，水电站信息
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ddh_hystation_id")
    @JsonBackReference
    private HydropowerStation hydropowerStation;
    // 关联子表，水库水位库容曲线
    @OneToMany(mappedBy = "reservoirInf", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ReservoirInfStorage> reservoirInfStorages;
}
