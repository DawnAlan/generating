package com.hust.generatingcapacity.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Data
@Getter
@Setter
public class CodeValue {

    private double code;
    private double value;

    public CodeValue(double code, double value) {
        this.code = code;
        this.value = value;
    }

    public CodeValue() {
    }

    public static double linearInterpolation(double targetCode, List<CodeValue> values) {
        //先对其进行排序
        values.sort(Comparator.comparing(CodeValue::getCode));
        // 二分查找
        int index = Collections.binarySearch(values, new CodeValue(targetCode, 0),
                Comparator.comparing(CodeValue::getCode));
        CodeValue left = null;
        CodeValue right = null;
        if (index >= 0) {
            // 目标值等于某个 code
            left = values.get(index);
            right = index < values.size() - 1 ? values.get(index + 1) : null;
        } else {
            // 目标值不等于任何 code
            int insertionPoint = -index - 1;
            if (insertionPoint <= 0) {
                // 小于最小值，直接取第一个
                left = values.get(0);
            } else if (insertionPoint >= values.size()) {
                // 大于最大值，直接取最后一个
                right = values.get(values.size() - 1);
            } else {
                // 比较插入点前后哪个更接近
                left = values.get(insertionPoint - 1);
                right = values.get(insertionPoint);
            }
        }
        if (left == null && right != null) {
            return right.getValue();
        } else if (left != null && right == null) {
            return left.getValue();
        } else {
            if (left.getCode() == right.getCode()) {
                return left.getValue();
            } else {
                return left.getValue() + (targetCode - left.getCode()) / (right.getCode() - left.getCode()) * (right.getValue() - left.getValue());
            }
        }
    }

    public static double getMaxCode(List<CodeValue> values){
        return Collections.max(values, Comparator.comparing(CodeValue::getCode)).getCode();
    }
    public static double getMaxValue(List<CodeValue> values){
        return Collections.max(values, Comparator.comparing(CodeValue::getValue)).getValue();
    }
}
