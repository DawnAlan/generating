package com.hust.generatingcapacity.dto;

import com.hust.generatingcapacity.model.generation.domain.CodeValue;
import com.hust.generatingcapacity.model.generation.domain.NHQData;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Data
@Getter
@Setter
public class NHQCellDTO {
    private double h;
    private double q;
    private double n;
    private Double max_p;//最大出力
    private Double min_p;//稳定运行下限
    private Double max_q;//最大流量
    private Double min_q;//稳定运行用水


    public NHQCellDTO(double h, double q, double n, Double max_p, Double min_p, Double max_q, Double min_q) {
        this.h = h;
        this.q = q;
        this.n = n;
        this.max_p = max_p;
        this.min_p = min_p;
        this.max_q = max_q;
        this.min_q = min_q;
    }

    public NHQCellDTO() {
    }


}
