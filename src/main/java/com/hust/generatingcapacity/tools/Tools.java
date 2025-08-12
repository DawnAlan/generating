package com.hust.generatingcapacity.tools;

import java.util.List;

public class Tools {

    /**
     * 检查object是什么类型
     * @param obj
     * @return
     */
    public static Integer checkObjectType(Object obj){
        int type = 0;
        if (obj instanceof String) {
            type = 1;
        } else if (obj instanceof Number) {
            type = 2;
        }else if (obj instanceof Boolean) {
            type = 3;
        }else if (obj instanceof java.util.Date) {
            type = 4;
        }
        return type;
    }

    /**
     * 检查是否数组越界的方法
     * @param array
     * @param row
     * @param column
     * @return
     */
    public static boolean isInBounds(Object[][] array, int row, int column) {
        if (array == null) {
            return false;
        }

        // 检查行索引是否在有效范围内
        if (row < 0 || row >= array.length) {
            return false;
        }

        // 检查列索引是否在有效范围内
        if (column < 0 || column >= array[row].length) {
            return false;
        }

        // 如果行和列都在有效范围内
        return true;
    }

    /**
     * 把两个Object相加，除了两者都是数字时相加，其他时候都是转成字符串相加
     * @param a
     * @param b
     * @return
     */
    public static Object addObject(Object a ,Object b){
        Object r = new Object();
        if (a==null){
            a = 0;
        }
        if (b==null){
            b = 0;
        }
        if (a instanceof Number && b instanceof Number) {
            if (a instanceof Integer && b instanceof Double) {
                int aValue = (Integer) a;
                double bValue = ((Double) b);
                r = aValue+bValue;
            } else if (a instanceof Integer && b instanceof Integer) {
                int aValue = (Integer) a;
                int bValue = ((Integer) b);
                r = aValue+bValue;
            }else if (a instanceof Double && b instanceof Integer) {
                double aValue = (Double) a;
                int bValue = ((Integer) b);
                r = aValue+bValue;
            } else if (a instanceof Double && b instanceof Double) {
                double aValue = (Double) a;
                double bValue = ((Double) b);
                r = aValue+bValue;
            }
        }else {
            r = a + b.toString();
        }
        return r;
    }

    /**
     * 求数组中的最大值
     *
     * @param array 一维的数组
     * @return 数组最大值
     */
    public static double max(double[] array) {
        int length = array.length;
        int tem = 0;
        for (int i = 1; i < length; i++)
            if (array[i] > array[tem]) tem = i;
        return array[tem];
    }

    /**
     * 将List<List<Double>> 转换为Object，一维List代表某一列的所有行，二维List代表有几列
     * @param data
     * @return
     */
    public static Object[][] listTwoToObject(List<List<Double>>data){
        int l = 0;
        for (List<Double> datum : data) {
            if (datum.size() > l) {
                l = datum.size();
            }
        }
        Object[][] array = new Object[l][data.size()];
        for (int i = 0; i < data.size(); i++) {
            for (int j = 0; j < data.get(i).size(); j++) {
                array[j][i] = data.get(i).get(j);
            }
        }
        return array;
    }
}
