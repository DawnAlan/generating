package com.hust.generatingcapacity.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Data
@Getter
@Setter
public class HNQData {

    private double head;
    private List<CodeValue> NQ = new ArrayList<>();

    public HNQData(double head, List<CodeValue> NQ) {
        this.head = head;
        this.NQ = NQ;
    }

    public HNQData(double head) {
        this.head = head;
    }

    public HNQData() {
    }

    /**
     * Q为codeValue中的code，求N;也可以直接完全反过来
     */
    public static double lineInterpolation(double headCode, double q, List<HNQData> values) {
        //先对其进行排序
        values.sort(Comparator.comparing(HNQData::getHead));
        // 二分查找
        int index = Collections.binarySearch(values, new HNQData(headCode),
                Comparator.comparing(HNQData::getHead));
        HNQData left = null;
        HNQData right = null;
        if (index >= 0) {
            // 目标值等于某个 code
            left = values.get(index);
            right = index < values.size() - 1 ? values.get(index + 1) : null;
        } else {
            // 目标值不等于任何 code
            int insertionPoint = -index - 1;
            if (insertionPoint > 0) {
                left = values.get(insertionPoint - 1);
            }
            if (insertionPoint < values.size()) {
                right = values.get(insertionPoint);
            }
        }
        //几种可能情况
        if (left == null && right != null) {
            List<CodeValue> NQ = right.getNQ();
            return CodeValue.linearInterpolation(q, NQ);
        } else if (left != null && right == null) {
            List<CodeValue> NQ = left.getNQ();
            return CodeValue.linearInterpolation(q, NQ);
        } else {
            if (left.getHead() == right.getHead()) {//以防万一
                List<CodeValue> NQ = left.getNQ();
                return CodeValue.linearInterpolation(q, NQ);
            } else {
                List<CodeValue> NQLeft = left.getNQ();
                List<CodeValue> NQRight = right.getNQ();
                double nLeft = CodeValue.linearInterpolation(q, NQLeft);
                double nRight = CodeValue.linearInterpolation(q, NQRight);
                return nLeft + (headCode - left.getHead()) / (right.getHead() - left.getHead()) * (nRight - nLeft);
            }
        }
    }

    public static double getMaxCode(double head, List<HNQData> values) {//NHQ向上取小
        //先对其进行排序
        values.sort(Comparator.comparing(HNQData::getHead));
        // 二分查找
        int index = Collections.binarySearch(values, new HNQData(head),
                Comparator.comparing(HNQData::getHead));
        HNQData data = null;
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
                HNQData lower = values.get(insertionPoint - 1);
                HNQData upper = values.get(insertionPoint);
                // 比较插入点前后哪个更接近
//                data = (Math.abs(head - lower.getHead()) <= Math.abs(head - upper.getHead())) ? lower : upper;
                // 向上取小
                data = lower;

            }
        }
        return CodeValue.getMaxCode(data.getNQ());
    }

    public static double getMaxValue(double head, List<HNQData> values) {
        //先对其进行排序
        values.sort(Comparator.comparing(HNQData::getHead));
        // 二分查找
        int index = Collections.binarySearch(values, new HNQData(head),
                Comparator.comparing(HNQData::getHead));
        HNQData data = null;
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
                HNQData lower = values.get(insertionPoint - 1);
                HNQData upper = values.get(insertionPoint);
                // 比较插入点前后哪个更接近
//                data = (Math.abs(head - lower.getHead()) <= Math.abs(head - upper.getHead())) ? lower : upper;
                // 向上取小
                data = lower;
            }
        }
        return CodeValue.getMaxValue(data.getNQ());
    }


}
