package com.hust.generatingcapacity.model.generation.type;

public enum ConstraintType {

    Water_Level_Fluctuation("水位变化"),
    Water_Level_Range("水位区间"),
    Flow_Interval("流量区间"),
    Channel_Constraint("通道约束"),
    Load_Interval("负荷区间"),
    Reservoir_Characteristic("水库特征");


    private final String desc;

    ConstraintType(String desc) {
        this.desc = desc;
    }

    public static ConstraintType fromCode(String code) {
        return switch (code.toLowerCase()) {
            case "水位区间" -> Water_Level_Range;
            case "水位变化" -> Water_Level_Fluctuation;
            case "流量区间" -> Flow_Interval;
            case "通道约束" -> Channel_Constraint;
            case "负荷区间" -> Load_Interval;
            case "水库特征" -> Reservoir_Characteristic;
            default -> Flow_Interval;
        };
    }
}
