package com.hust.generatingcapacity.model.generation.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Data
@Getter
@Setter
public class NHQCell {

    private double h;
    private double q;
    private double n;
    private Double max_p;//最大出力
    private Double min_p;//稳定运行下限
    private Double max_q;//最大流量
    private Double min_q;//稳定运行用水


    public NHQCell(double h, double q, double n) {
        this.h = h;
        this.q = q;
        this.n = n;
    }

    public NHQCell(double h, double q, double n, Double max_p, Double min_p, Double max_q, Double min_q) {
        this.h = h;
        this.q = q;
        this.n = n;
        this.max_p = max_p;
        this.min_p = min_p;
        this.max_q = max_q;
        this.min_q = min_q;
    }

    public NHQCell() {
    }



    public static List<NHQData> convert(List<NHQCell> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        List<NHQData> NHQDataList = values.stream()
                .collect(Collectors.groupingBy(NHQCell::getH))
                .entrySet()
                .stream()
                .map(entry -> {
                    double head = entry.getKey();
                    List<NHQCell> group = entry.getValue();
                    // 计算上下限
                    double max_n = getSafeValue(group, NHQCell::getMax_p, NHQCell::getN, true);
                    double max_q = getSafeValue(group, NHQCell::getMax_q, NHQCell::getQ, true);
                    double min_n = getSafeValue(group, NHQCell::getMin_p, NHQCell::getN, false);
                    double min_q = getSafeValue(group, NHQCell::getMin_q, NHQCell::getQ, false);
                    // NQ 曲线：N -> Q
                    List<CodeValue> NQ = group.stream()
                            .sorted(Comparator.comparingDouble(NHQCell::getN))
                            .map(cell -> new CodeValue(cell.getN(), cell.getQ()))
                            .filter(cv -> cv.getCode() >= min_n) // 只保留大于等于稳定负荷的点
                            .collect(Collectors.toList());
                    // 把稳定点和最大点加入 NQ
                    if (!Double.isNaN(min_n) && !Double.isNaN(min_q)) {
                        NQ.add(new CodeValue(min_n, min_q));
                    }
                    if (!Double.isNaN(max_n) && !Double.isNaN(max_q)) {
                        NQ.add(new CodeValue(max_n, max_q));
                    }
                    NQ.sort(Comparator.comparingDouble(CodeValue::getCode));
                    // QN 曲线：Q -> N
                    List<CodeValue> QN = group.stream()
                            .sorted(Comparator.comparingDouble(NHQCell::getQ))
                            .map(cell -> new CodeValue(cell.getQ(), cell.getN()))
                            .filter(cv -> cv.getCode() >= min_q) // 只保留大于等于稳定用水的点
                            .collect(Collectors.toList());
                    // 把稳定点和最大点加入 QN
                    if (!Double.isNaN(min_q) && !Double.isNaN(min_n)) {
                        QN.add(new CodeValue(min_q, min_n));
                    }
                    if (!Double.isNaN(max_q) && !Double.isNaN(max_n)) {
                        QN.add(new CodeValue(max_q, max_n));
                    }
                    QN.sort(Comparator.comparingDouble(CodeValue::getCode));
                    return new NHQData(head, NQ, QN);
                })
                .sorted(Comparator.comparingDouble(NHQData::getHead)) // 按 head 排序（可选）
                .collect(Collectors.toList());
        return NHQDataList;
    }

    // 公共方法：从 group 中安全计算最大/最小值
    private static double getSafeValue(
            List<NHQCell> group,
            Function<NHQCell, Double> preferredGetter, // 首选值（例如 getMax_p）
            ToDoubleFunction<NHQCell> streamGetter,    // 备用计算（例如 cell -> cell.getN()）
            boolean isMax                               // true=最大值, false=最小值
    ) {
        return Optional.ofNullable(group)
                .filter(g -> !g.isEmpty())
                .map(g -> {
                    Double preferred = preferredGetter.apply(g.get(0));
                    return Objects.requireNonNullElseGet(preferred, () -> (isMax
                            ? g.stream().mapToDouble(streamGetter).max()
                            : g.stream().mapToDouble(streamGetter).min()
                    ).orElse(Double.NaN));
                })
                .orElse(Double.NaN);
    }


}
