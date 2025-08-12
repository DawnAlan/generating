package com.hust.generatingcapacity.pre_processing;

import com.hust.generatingcapacity.entity.TimeRange;
import com.hust.generatingcapacity.tools.ExcelUtils;

import java.math.BigDecimal;
import java.util.*;

public class Generating {
    public static void main(String[] args) {
        Object[][] data1 = ExcelUtils.readExcel("D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\发电资料\\大渡河日电量.xlsx", "Sheet1");
        Object[][] data2 = ExcelUtils.readExcel("D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\发电资料\\大渡河日电量.xlsx", "Sheet2");
        Map<String, Integer> splitsPositions = Map.of("split1", 0, "time", 1, "split2", 2);
        Map<Integer, String> splitMessages = new TreeMap<>();
        for (Object[] objects : data2) {
            if (objects[0] instanceof Integer && objects[1] instanceof String) {
                splitMessages.put((Integer) objects[0], (String) objects[1]);
            }
        }
        TimeRange timeRange = new TimeRange();
        Integer split1 = 11000000;
        Integer split2 = 2000;
        Object[][] result = splitData(data1, splitsPositions, timeRange, splitMessages, split1, split2);
        ExcelUtils.writeExcel("D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\发电资料\\大渡河日电量.xlsx", splitMessages.get(split1) + splitMessages.get(split2), result);
        StringBuilder sb = new StringBuilder();
        sb.append("已将数据按照筛选条件拆分，生成新的sheet，sheet名称为：").append(splitMessages.get(split1)).append(splitMessages.get(split2)).append("\n");
        if (!timeRange.isEmpty()){
            sb.append("时间范围：").append(timeRange.getStart()).append(" - ").append(timeRange.getEnd()).append("\n");
        }
        sb.append("数据量：").append(result.length - 1).append("\n");
        System.out.println(sb);
    }

    /**
     * 根据时间范围和其他筛选条件对数据进行筛选
     *
     * @param data            数据，二维数组，第一行是表头
     * @param splitsPositions 筛选条件所在的列索引，key 是列名，value 是列索引
     *                        例如：{"time": 0, "split1": 1, "split2": 2}
     * @param timeRange       时间范围
     * @param splitMessages   筛选条件对应的值，key 是筛选条件的值，value 是对应的描述
     *                        例如：{10086: "A", 204: "B"}
     * @param splits          筛选条件的值，顺序要和 splitsPositions 中的顺序一致
     *                        例如：[10086, 204]
     * @return 筛选后的数据，二维数组，第一行是表头
     */
    public static Object[][] splitData(Object[][] data, Map<String, Integer> splitsPositions, TimeRange timeRange, Map<Integer, String> splitMessages, Integer... splits) {
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
            if (timeIndex != -10086 && timeRange != null && !timeRange.isEmpty()) {
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
