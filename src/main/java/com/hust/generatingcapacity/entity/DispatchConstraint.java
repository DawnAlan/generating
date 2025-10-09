package com.hust.generatingcapacity.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.util.List;

@Data
@Entity
@Table(name = "ddh_hystation_constraint")
@Comment("电站发电约束")
public class DispatchConstraint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "constraint_type", columnDefinition = "TEXT COMMENT '约束类型'")
    private String constraintType;
    @Column(name = "description", columnDefinition = "TEXT COMMENT '约束描述'")
    private String description;
    @Column(name = "is_active", columnDefinition = "bit(1) COMMENT '是否启用'")
    private Boolean isActive;
    @Column(name = "is_rigid", columnDefinition = "bit(1) COMMENT '是否刚性约束'")
    private Boolean isRigid;
    // 关联父表，水电站信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ddh_hystation_id")
    @JsonBackReference
    private HydropowerStation hydropowerStation;
    // 关联子表，生效条件
    @OneToMany(mappedBy = "dispatchConstraint", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("constraint-condition")
    private List<ConstraintCondition> constraintConditions;
    // 关联子表，约束参数
    @OneToMany(mappedBy = "dispatchConstraint", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("constraint-param")
    private List<ConstraintParam> constraintParams;


}
