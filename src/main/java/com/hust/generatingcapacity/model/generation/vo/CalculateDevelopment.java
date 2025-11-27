package com.hust.generatingcapacity.model.generation.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Data
@Getter
@Setter
public class CalculateDevelopment {
    //流域名称
    public String basin;
    //开始日期
    public Date startDate;
    //结束日期
    public Date endDate;
    //调度方法(规程优化、规则调度、预设条件)
    public String DispatchType;
    //调度尺度
    public String period;
    //输入数据
    public Map<String, CalculateInput> calculateInputs;
    //预设条件
    public Map<String, CalculateCondition> calculateConditions;
}
