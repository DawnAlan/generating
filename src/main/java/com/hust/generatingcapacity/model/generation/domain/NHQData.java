package com.hust.generatingcapacity.model.generation.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.Function;

@Data
@Getter
@Setter
public class NHQData {

    private double head;
    private List<CodeValue> NQ = new ArrayList<>();
    private List<CodeValue> QN = new ArrayList<>();

    public NHQData(double head, List<CodeValue> NQ, List<CodeValue> QN) {
        this.head = head;
        this.NQ = NQ;
        this.QN = QN;
    }


    public NHQData(double head) {
        this.head = head;
    }

    public NHQData() {
    }

    /**
     * Q为codeValue中的code，求N;也可以直接完全反过来
     */
    public static double lineInterpolation(double head, double code, String key, List<NHQData> values) {
        // 按 head 排序
        values.sort(Comparator.comparing(NHQData::getHead));
        // 二分查找
        int index = Collections.binarySearch(values, new NHQData(head),
                Comparator.comparing(NHQData::getHead));
        NHQData left = null, right = null;
        if (index >= 0) {
            left = values.get(index);
            right = (index < values.size() - 1) ? values.get(index + 1) : null;
        } else {
            int insertionPoint = -index - 1;
            if (insertionPoint > 0) left = values.get(insertionPoint - 1);
            if (insertionPoint < values.size()) right = values.get(insertionPoint);
        }
        // 定义曲线选择器（避免写多次 switch）
        Map<String, Function<NHQData, List<CodeValue>>> curveMap = Map.of(
                "Q", NHQData::getNQ,
                "N", NHQData::getQN
        );
        Function<NHQData, List<CodeValue>> curveSelector = curveMap.get(key);
        if (curveSelector == null) {
            throw new IllegalArgumentException("不支持的 key: " + key);
        }
        // 单侧存在
        if (left == null) return CodeValue.linearInterpolation(code, curveSelector.apply(right));
        if (right == null) return CodeValue.linearInterpolation(code, curveSelector.apply(left));
        // 两侧 head 相等（退化情况）
        if (left.getHead() == right.getHead()) {
            return CodeValue.linearInterpolation(code, curveSelector.apply(left));
        }
        // 双侧插值
        double leftVal = CodeValue.linearInterpolation(code, curveSelector.apply(left));
        double rightVal = CodeValue.linearInterpolation(code, curveSelector.apply(right));
        return leftVal + (head - left.getHead()) / (right.getHead() - left.getHead()) * (rightVal - leftVal);
    }


    public static double getMaxN(double head, List<NHQData> values) {//NHQ向上取小水头
        //先对其进行排序
        values.sort(Comparator.comparing(NHQData::getHead));
        // 二分查找
        int index = Collections.binarySearch(values, new NHQData(head),
                Comparator.comparing(NHQData::getHead));
        NHQData data = null;
        if (index >= 0) {
            // 目标值等于某个 code
            data = values.get(index);
        } else {
            // 目标值不等于任何 code
            int insertionPoint = -index - 1;
            if (insertionPoint <= 0) {
                // 小于最小值，直接取第一个
                data = values.get(0);
            } else if (insertionPoint >= values.size()) {
                // 大于最大值，直接取最后一个
                data = values.get(values.size() - 1);
            } else {
                NHQData lower = values.get(insertionPoint - 1);
                NHQData upper = values.get(insertionPoint);
                // 比较插入点前后哪个更接近
//                data = (Math.abs(head - lower.getHead()) <= Math.abs(head - upper.getHead())) ? lower : upper;
                // 向上取小
                data = lower;

            }
        }
        return CodeValue.getMaxCode(data.getNQ());
    }

    public static double getMaxQ(double head, List<NHQData> values) {
        //先对其进行排序
        values.sort(Comparator.comparing(NHQData::getHead));
        // 二分查找
        int index = Collections.binarySearch(values, new NHQData(head),
                Comparator.comparing(NHQData::getHead));
        NHQData data = null;
        if (index >= 0) {
            // 目标值等于某个 code
            data = values.get(index);
        } else {
            // 目标值不等于任何 code
            int insertionPoint = -index - 1;
            if (insertionPoint <= 0) {
                // 小于最小值，直接取第一个
                data = values.get(0);
            } else if (insertionPoint >= values.size()) {
                // 大于最大值，直接取最后一个
                data = values.get(values.size() - 1);
            } else {
                NHQData lower = values.get(insertionPoint - 1);
                NHQData upper = values.get(insertionPoint);
                // 比较插入点前后哪个更接近
//                data = (Math.abs(head - lower.getHead()) <= Math.abs(head - upper.getHead())) ? lower : upper;
                // 向上取小
                data = lower;
            }
        }
        return CodeValue.getMaxValue(data.getNQ());
    }


}
