package com.hust.generatingcapacity.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.util.List;

@Data
@Entity
@Table(name = "ddh_hystation_unit_curve")
@Comment("机组NHQ曲线")
public class UnitInfNhqCurve {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "curve_name", columnDefinition = "TEXT COMMENT '曲线名称'")
    private String curveName;
    // 关联子表，机组信息
    @OneToMany(mappedBy = "unitInfNhqCurve", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("curve-inf")
    @OrderBy("id ASC")
    private List<UnitInf> unitInfs;
    // 关联子表，机组NHQ曲线点
    @OneToMany(mappedBy = "unitInfNhqCurve", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("curve-point")
    @OrderBy("head ASC, flow ASC")
    private List<UnitInfNhqCurvePoint> unitInfNhqCurvePoints;
}
