package com.hust.generatingcapacity.model.generation.type;

public enum PreConditionType {
    //预设条件类型
    H_after("预设末水位"),
    V_after("预设蓄能比"),
    Q_power("预设发电流量"),;

    private final String desc;

    PreConditionType(String desc) {
        this.desc = desc;
    }

}
