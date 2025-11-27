package com.hust.generatingcapacity.model.generation.vo;

import com.hust.generatingcapacity.model.generation.type.PreConditionType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Data
@Getter
@Setter
public class CalculateCondition {
    //预设条件所针对电站
    public String station;
    //预设条件类型
    public PreConditionType preCondition;
    //条件结束时间
    public Date endTime;
    //预设条件值
    public Double preValue;
}
