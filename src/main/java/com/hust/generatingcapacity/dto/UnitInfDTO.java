package com.hust.generatingcapacity.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class UnitInfDTO {
    private Integer id;
    //机组名称
    private String unitName;
    //单机容量(MW)
    private Double unitCapacity;
    //是否投产
    private Boolean status;
    //NHQ曲线名称
    private String curveName;
    //NHQ曲线信息
    private List<NHQCellDTO> nhqLine;

    public UnitInfDTO(Integer id, String unitName, Double unitCapacity, Boolean status, String curveName) {
        this.id = id;
        this.unitName = unitName;
        this.unitCapacity = unitCapacity;
        this.status = status;
        this.curveName = curveName;
    }

    public UnitInfDTO() {
    }

    @Override
    public String toString() {
        return "{" +
                "机组型号='" + unitName + '\'' +
                ", 单机容量=" + unitCapacity +
                ", 是否投产=" + status +
                ", 曲线='" + curveName + '\'' +
                '}';
    }
}
