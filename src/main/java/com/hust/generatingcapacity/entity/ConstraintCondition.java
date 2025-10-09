package com.hust.generatingcapacity.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Entity
@Table(name = "ddh_hystation_constraint_condition")
@Comment("约束生效条件")
public class ConstraintCondition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "condition_name", columnDefinition = "TEXT COMMENT '生效条件（T-时间、H-水位、L-预见期、dL-时段、Qin-入库流量）'")
    private String conditionName;
    @Column(name = "operator", columnDefinition = "TEXT COMMENT '运算符（>,>=,==,<=,<）'")
    private String operator;
    @Column(name = "connector", columnDefinition = "TEXT COMMENT '连接符（||,&&）'")
    private String connector;
    @Column(name = "value", columnDefinition = "DOUBLE COMMENT '值'")
    private Double value;
    @Column(name = "unit", columnDefinition = "TEXT COMMENT '单位（d,m）'")
    private String unit;
    @Column(name = "remark", columnDefinition = "TEXT COMMENT '备注'")
    private String remark;
    // 关联父表，电站发电约束
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ddh_hystation_constraint_id")
    @JsonBackReference("constraint-condition")
    private DispatchConstraint dispatchConstraint;
}
