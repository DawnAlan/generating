package com.hust.generatingcapacity.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Entity
@Table(name = "ddh_hystation_constraint_param")
@Comment("发电约束参数")
public class ConstraintParam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "param_name", columnDefinition = "TEXT COMMENT '约束参数（H、dH、P-负荷、Qp-发电、Qo-出库、C-通道容量）'")
    private String paramName;
    @Column(name = "operator", columnDefinition = "TEXT COMMENT '运算符（>,>=,=,<=,<）'")
    private String operator;
    @Column(name = "value", columnDefinition = "varchar(255) COMMENT '值'")
    private String value;
    @Column(name = "unit", columnDefinition = "TEXT COMMENT '单位（m,m/d,m/w,m³/s,WM）'")
    private String unit;
    @Column(name = "remark", columnDefinition = "TEXT COMMENT '备注'")
    private String remark;
    // 关联父表，电站发电约束
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ddh_hystation_constraint_id")
    @JsonBackReference("constraint-param")
    private DispatchConstraint dispatchConstraint;

    //获取该约束的表达式
    public String getExpression() {
        return paramName + " " + operator + " " + value;
    }

}
