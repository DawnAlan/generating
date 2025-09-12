package com.hust.generatingcapacity.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Entity
@Table(name = "ddh_hystation_unit")
@Comment("机组特征信息")
public class UnitInf {// 机组信息
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "unit_name", columnDefinition = "TEXT COMMENT '机组名称'")
    private String unitName;
    @Column(name = "unit_capacity", columnDefinition = "DOUBLE COMMENT '单机容量(MW)'")
    private Double unitCapacity;
    @Column(name = "status", columnDefinition = "bit(1) COMMENT '是否投产'")
    private Boolean status;
    // 关联父表，水电站信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ddh_hystation_id")
    @JsonBackReference
    private HydropowerStation hydropowerStation;
    //关联父表，机组NHQ曲线
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ddh_hystation_unit_curve_id")
    @JsonBackReference("curve-inf")
    private UnitInfNhqCurve unitInfNhqCurve;

}
