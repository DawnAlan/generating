package com.hust.generatingcapacity.pre_processing;

import com.hust.generatingcapacity.entity.TimeRange;
import com.hust.generatingcapacity.tools.ExcelUtils;
import com.hust.generatingcapacity.tools.PreProcessUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;



public class Generating {
    public static void main(String[] args) throws ParseException {
        String fileName = "C:\\Users\\12566\\Desktop\\大渡河数据\\发电资料\\大渡河日电量.xlsx";
        Object[][] data1 = ExcelUtils.readExcel(fileName, "Sheet1");
        Object[][] data2 = ExcelUtils.readExcel(fileName, "Sheet2");
        Map<String, Integer> splitsPositions = Map.of("split1", 0, "time", 1, "split2", 2);
        Map<Integer, String> splitMessages = new TreeMap<>();
        for (Object[] objects : data2) {
            if (objects[0] instanceof Integer && objects[1] instanceof String) {
                splitMessages.put((Integer) objects[0], (String) objects[1]);
            }
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
        TimeRange timeRange = new TimeRange(sdf.parse("2024-06-01 00"),sdf.parse("2024-09-30 23"));
        List<Integer> splits = new ArrayList<>();
        splits.add(11000324);
        splits.add(11000029);
        splits.add(11000004);
        splits.add(11000005);
        splits.add(11000030);
        splits.add(11000325);
        splits.add(11000001);
        splits.add(11000000);
//        Integer split1 = 11000000;
        Integer split2 = 2000;
        String fileName2 = "C:\\Users\\12566\\Desktop\\大渡河数据\\发电资料\\大渡河24年汛期日电量.xlsx";
        for (Integer split : splits) {
            Object[][] result = PreProcessUtils.splitData(data1, splitsPositions, timeRange, splitMessages, split, split2);
            ExcelUtils.writeExcel(fileName2, splitMessages.get(split) + splitMessages.get(split2), result);
            StringBuilder sb = new StringBuilder();
            sb.append("已将数据按照筛选条件拆分，生成新的sheet，sheet名称为：").append(splitMessages.get(split)).append(splitMessages.get(split2)).append("\n");
            if (!timeRange.isEmpty()) {
                sb.append("时间范围：").append(timeRange.getStart()).append(" - ").append(timeRange.getEnd()).append("\n");
            }
            sb.append("数据量：").append(result.length - 1).append("\n");
            System.out.println(sb);
        }

    }



}
