package com.hust.generatingcapacity.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Getter
@Setter
public class NHQCell {
    private double h;
    private double q;
    private double n;

    public NHQCell(double h, double q, double n) {
        this.h = h;
        this.q = q;
        this.n = n;
    }

    public NHQCell() {
    }

    public static List<HNQData> convert(String value, List<NHQCell> values) {
        List<HNQData> hnqDataList = switch (value) {
            case "Q" ->
                // 排序 + 分组 + 转换为 List<HNQData>
                    values.stream()
                            .sorted(Comparator.comparingDouble(NHQCell::getH).thenComparing(NHQCell::getN)) // 按 h 排序
                            .collect(Collectors.groupingBy(
                                    NHQCell::getH,
                                    HashMap::new,
                                    Collectors.mapping(
                                            cell -> new CodeValue(cell.getN(), cell.getQ()),
                                            Collectors.toList()
                                    )
                            ))
                            .entrySet()
                            .stream()
                            .map(entry -> new HNQData(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toList());
            case "N" -> values.stream()//Q为key，N为value
                    .sorted(Comparator.comparingDouble(NHQCell::getH).thenComparing(NHQCell::getQ)) // 按 h 排序
                    .collect(Collectors.groupingBy(
                            NHQCell::getH,
                            HashMap::new,
                            Collectors.mapping(
                                    cell -> new CodeValue(cell.getQ(), cell.getN()),
                                    Collectors.toList()
                            )
                    ))
                    .entrySet()
                    .stream()
                    .map(entry -> new HNQData(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
            default -> throw new RuntimeException("不能处理除N/Q外的逻辑");
        };
        return hnqDataList;
    }

}
