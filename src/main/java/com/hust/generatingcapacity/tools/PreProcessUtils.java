package com.hust.generatingcapacity.tools;

import com.hust.generatingcapacity.model.common.TimeRange;

import java.util.*;

public class PreProcessUtils {

    /**
     * 根据时间范围和其他筛选条件对数据进行筛选
     *
     * @param data            数据，二维数组，第一行是表头
     * @param splitsPositions 筛选条件所在的列索引，key 是列名，value 是列索引
     *                        例如：{"time": 0, "split1": 1, "split2": 2}
     * @param timeRange       时间范围
     * @param splitMessages   筛选条件对应的值，key 是筛选条件的值，value 是对应的描述
     *                        例如：{10086: "A", 204: "B"}
     * @return 筛选后的数据，二维数组，第一行是表头
     */
    @SafeVarargs
    public static <T> Object[][] splitData(Object[][] data, Map<String, Integer> splitsPositions, TimeRange timeRange, Map<T, String> splitMessages, T... splits) {
        List<Object[]> splitData = new ArrayList<>();
        //获取到时间和其它筛选条件所在的列索引
        Integer timeIndex = -10086;
        List<Integer> splitList = new ArrayList<>();
        List<String> splitPositionsKeys = new ArrayList<>(splitsPositions.keySet());
        splitPositionsKeys.sort((a, b) -> {
            // time 永远排到最后
            if ("time".equals(a)) return 1;
            if ("time".equals(b)) return -1;
            // 提取 split 后面的数字
            int numA = Integer.parseInt(a.replace("split", ""));
            int numB = Integer.parseInt(b.replace("split", ""));
            return Integer.compare(numA, numB);
        });
        for (String key : splitPositionsKeys) {
            if ("time".equals(key)) {
                timeIndex = splitsPositions.get(key);
            } else {
                splitList.add(splitsPositions.get(key));
            }
        }
        //根据 splits 中的索引进行数据筛选
        Object[] head = data[0];
        splitData.add(head);
        for (int i = 1; i < data.length; i++) {
            boolean match = true;
            if (timeIndex != -10086 && timeRange != null && !timeRange.isEmpty() && Tools.checkObjectType(data[i][timeIndex]) == 4) {
                match = TimeRange.isInRange((Date) data[i][timeIndex], timeRange);
            }
            for (int j = 0; j < splitList.size(); j++) {
                if (!Objects.equals(splits[j], data[i][splitList.get(j)])) {
                    match = false;
                    break;
                } else {
                    data[i][splitList.get(j)] = splitMessages.get(splits[j]);
                }
            }
            if (match) {
                splitData.add(data[i]);
            }
        }
        return splitData.toArray(new Object[0][splitData.size()]);
    }
}
