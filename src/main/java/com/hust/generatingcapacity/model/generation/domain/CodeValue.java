package com.hust.generatingcapacity.model.generation.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    public static List<CodeValue> enlargeStep(List<CodeValue> list, int factor) {
        List<CodeValue> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += factor) {
            result.add(list.get(i));
        }
        if (!result.contains(list.get(list.size() - 1))) {
            result.add(list.get(list.size() - 1)); // 确保包含尾部
        }
        return result;
    }

    public static List<CodeValue> exchangeCopy(List<CodeValue> original) {
        List<CodeValue> result = new ArrayList<>(original.stream()
                .map(cv -> new CodeValue(cv.getValue(), cv.getCode()))
                .collect(Collectors.toList()));  // 先获取不可变的列表，再转换为可变列表
        return result;
    }

    public static double codeDifference(double value1, double value2, List<CodeValue> values) {
        values = exchangeCopy(values);
        return difference(value1, value2, values);
    }

    public static double difference(double code1, double code2, List<CodeValue> values) {
        double value1 = linearInterpolation(code1, values);
        double value2 = linearInterpolation(code2, values);
        return value2 - value1;
    }

    public static void sampleToGrid(List<CodeValue> value1, List<CodeValue> value2) {
        if (value1.isEmpty() || value2.isEmpty()) {
            throw new IllegalArgumentException("重采样数据不能为空！");
        }
        if (value1.size() == 1 && value2.size() == 1) {
            CodeValue cv1 = value1.get(0);
            cv1.setCode(value2.get(0).getCode());
        }
        if (value1.size() == 1 && value2.size() > 1) {
            CodeValue cv1 = value1.get(0);
            cv1.setCode(value2.get(0).getCode());
            for (int i = 1; i < value2.size(); i++) {
                value1.add(i, new CodeValue(value2.get(i).getCode(), value1.get(0).getValue()));
            }
        }
        if (value2.size() == 1 && value1.size() > 1) {
            CodeValue cv2 = value2.get(0);
            cv2.setCode(value1.get(0).getCode());
            for (int i = 1; i < value1.size(); i++) {
                value2.add(i, new CodeValue(value1.get(i).getCode(), value2.get(0).getValue()));
            }
        }
        if (value1.size() > 1 && value2.size() > 1) {
            double minCode = Math.min(getMinCode(value1), getMinCode(value2));
            double maxCode = Math.max(getMaxCode(value1), getMaxCode(value2));
            double step = Math.min(value1.get(1).getCode() - value1.get(0).getCode(), value2.get(1).getCode() - value2.get(0).getCode());
            if (getMinCode(value1) > minCode) {
                value1.add(0, new CodeValue(minCode, linearInterpolation(minCode, value1)));
            }
            if (getMinCode(value2) > minCode) {
                value2.add(0, new CodeValue(minCode, linearInterpolation(minCode, value2)));
            }
            if (getMaxCode(value1) < maxCode) {
                value1.add(new CodeValue(maxCode, linearInterpolation(maxCode, value1)));
            }
            if (getMaxCode(value2) < maxCode) {
                value2.add(new CodeValue(maxCode, linearInterpolation(maxCode, value2)));
            }
            int l = (int) Math.ceil((maxCode - minCode) / step) + 1;
            List<CodeValue> newValue1 = new ArrayList<>();
            List<CodeValue> newValue2 = new ArrayList<>();
            for (int i = 0; i < l; i++) {
                newValue1.add( new CodeValue(minCode + i * step, linearInterpolation(minCode + i * step, value1)));
                newValue2.add( new CodeValue(minCode + i * step, linearInterpolation(minCode + i * step, value2)));
            }
            value1.clear();
            value1.addAll(newValue1);
            value2.clear();
            value2.addAll(newValue2);
        }
    }

    public static double linearInterpolation(double targetCode, List<CodeValue> values) {
        if (values.isEmpty()){
            return 0.0;
        }
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

    public static double codeLinearInterpolation(double targetValue, List<CodeValue> values) {
        //转换code-value
        List<CodeValue> exchangedValues = exchangeCopy(values);
        return  linearInterpolation(targetValue, exchangedValues);
    }

    public static double getMaxCode(List<CodeValue> values) {
        return Collections.max(values, Comparator.comparing(CodeValue::getCode)).getCode();
    }

    public static double getMinCode(List<CodeValue> values) {
        return Collections.min(values, Comparator.comparing(CodeValue::getCode)).getCode();
    }

    public static double getMaxValue(List<CodeValue> values) {
        return Collections.max(values, Comparator.comparing(CodeValue::getValue)).getValue();
    }

    public static double getMinValue(List<CodeValue> values) {
        return Collections.min(values, Comparator.comparing(CodeValue::getValue)).getValue();
    }

}
