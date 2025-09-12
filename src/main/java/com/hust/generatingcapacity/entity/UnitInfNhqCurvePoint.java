package com.hust.generatingcapacity.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Entity
@Table(name = "ddh_hystation_unit_curve_point")
@Comment("机组NHQ曲线点表")
public class UnitInfNhqCurvePoint {// 机组水头流量负荷曲线
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "head", columnDefinition = "DOUBLE COMMENT '水头（m）'")
    private Double head;
    @Column(name = "flow", columnDefinition = "DOUBLE COMMENT '流量（m³/s）'")
    private Double flow;
    @Column(name = "power", columnDefinition = "DOUBLE COMMENT '负荷（WM）'")
    private Double power;
    @Column(name = "max_flow", columnDefinition = "DOUBLE COMMENT '水头下最大流量（m³/s）'")
    private Double maxFlow;
    @Column(name = "max_power", columnDefinition = "DOUBLE COMMENT '水头下最大负荷（MW）'")
    private Double maxPower;
    @Column(name = "min_flow", columnDefinition = "DOUBLE COMMENT '稳定运行用水（m³/s）'")
    private Double minFlow;
    @Column(name = "min_power", columnDefinition = "DOUBLE COMMENT '稳定运行下限（MW）'")
    private Double minPower;
    // 关联父表，机组信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ddh_hystation_unit_curve_id")
    @JsonBackReference("curve-point")
    private UnitInfNhqCurve unitInfNhqCurve;

}
