package com.hust.generatingcapacity.dto;

import com.hust.generatingcapacity.model.generation.domain.CodeValue;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class ReservoirInfDTO {
    private Integer id;
    //水库名称
    private String reservoirName;
    //水库总库容（百万立方米）
    private Double totalCapacity;
    //水库调节库容（百万立方米）
    private Double effectiveCapacity;
    //水库死库容（百万立方米）
    private Double deadCapacity;
    //正常蓄水位（米）
    private Double normalWaterLevel;
    //校核洪水位（米）
    private Double checkFloodLevel = Double.MAX_VALUE;
    //最小库水位（米）
    private Double minRegulateLevel = 0.0;
    //水位库容曲线
    private List<CodeValueDTO> reservoirStorageLine;

    public ReservoirInfDTO(Integer id, String reservoirName, Double totalCapacity, Double effectiveCapacity, Double deadCapacity, Double normalWaterLevel, Double checkFloodLevel, Double minRegulateLevel) {
        this.id = id;
        this.reservoirName = reservoirName;
        this.totalCapacity = totalCapacity;
        this.effectiveCapacity = effectiveCapacity;
        this.deadCapacity = deadCapacity;
        this.normalWaterLevel = normalWaterLevel;
        this.checkFloodLevel = checkFloodLevel == null ? Double.MAX_VALUE : checkFloodLevel;
        this.minRegulateLevel = minRegulateLevel == null ? 0.0 : minRegulateLevel;
    }

    public ReservoirInfDTO() {
    }

    @Override
    public String toString() {
        return "{" +
                "水库名称='" + reservoirName + '\'' +
                ", 总库容=" + totalCapacity +
                ", 调节库容=" + effectiveCapacity +
                ", 死库容=" + deadCapacity +
                ", 正常蓄水位=" + normalWaterLevel +
                ", 校核洪水位=" + checkFloodLevel +
                ", 最小库水位=" + minRegulateLevel +
                '}';
    }
}
