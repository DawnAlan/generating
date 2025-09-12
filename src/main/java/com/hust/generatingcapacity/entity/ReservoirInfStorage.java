package com.hust.generatingcapacity.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Entity
@Table(name = "ddh_hystation_reservoir_storage")
@Comment("水库水位库容曲线")
public class ReservoirInfStorage {// 水库水位库容曲线
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "elevation", columnDefinition = "DOUBLE COMMENT '水位（m）'")
    private Double elevation;
    @Column(name = "capacity", columnDefinition = "DOUBLE COMMENT '库容（百万立方米）'")
    private Double capacity;
    // 关联父表，水电站库容特征
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ddh_hystation_reservoir_id")
    private ReservoirInf reservoirInf;

}
