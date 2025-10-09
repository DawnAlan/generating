package com.hust.generatingcapacity.model.generation.vo;

import com.hust.generatingcapacity.model.generation.type.ParamType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ParamValue {
    //约束参数类型
    private ParamType paramType;
    //约束参数值
    private Double paramValue;
    private String description;
    //是否为硬性约束
    private boolean isRigid;

    public ParamValue(ParamType paramType, Double paramValue, boolean isRigid) {
        this.paramType = paramType;
        this.paramValue = paramValue;
        this.isRigid = isRigid;
    }

    public ParamValue(ParamType paramType, Double paramValue, String description, boolean isRigid) {
        this.paramType = paramType;
        this.paramValue = paramValue;
        this.description = description;
        this.isRigid = isRigid;
    }

    public ParamValue() {
    }

    @Override
    public String toString() {
        return "约束{" +
                "约束参数类型=" + paramType +
                ", 约束参数值=" + paramValue +
                ", 硬性约束=" + isRigid +
                '}';
    }
}
