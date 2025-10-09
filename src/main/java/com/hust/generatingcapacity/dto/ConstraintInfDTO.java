package com.hust.generatingcapacity.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class ConstraintInfDTO {
    //id
    private Integer id;
    //约束类型
    private String constraintType;
    //硬性约束
    private Boolean isRigid;
    //约束描述
    private String description;
    //生效条件
    private String condition;
    //约束参数
    private List<String> param;

    public ConstraintInfDTO(Integer id, String constraintType, String description, Boolean isRigid) {
        this.id = id;
        this.constraintType = constraintType;
        this.description = description;
        this.isRigid = isRigid;
    }

    public ConstraintInfDTO() {
    }

    @Override
    public String toString() {
        return "{" +
                "约束类型='" + constraintType + '\'' +
                ", 约束描述='" + description + '\'' +
                ", 约束生效条件：'" + condition + '\'' +
                ", 约束参数：'" + param + '\'' +
                '}';
    }
}
