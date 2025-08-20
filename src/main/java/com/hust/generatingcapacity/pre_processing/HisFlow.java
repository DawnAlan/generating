package com.hust.generatingcapacity.pre_processing;


import com.hust.generatingcapacity.entity.HisStationData;
import com.hust.generatingcapacity.tools.ExcelUtils;

import java.text.ParseException;
import java.util.*;

import static org.apache.commons.lang3.time.DateUtils.parseDate;

public class HisFlow {
    public static void main(String[] args) throws ParseException {
        // 编码映射（点号定义.csv 读取）
        Object[][] codeMessage = ExcelUtils.readCsv("C:\\Users\\12566\\Desktop\\大渡河数据\\径流资料\\历史资料\\水文数据\\点号定义.csv");
        Map<String, String> splitMessages = new TreeMap<>();
        for (Object[] objects : codeMessage) {
            if (objects[0] instanceof String && objects[1] instanceof String) {
                splitMessages.put((String) objects[0], (String) objects[1]);
            }
        }

        // 所有结果
        Map<String, List<HisStationData>> stationData = new HashMap<>();

        // 中间临时结构：stationName -> (date -> HisStationData)
        Map<String, Map<Date, HisStationData>> tempMap = new HashMap<>();

        // 文件路径前缀
        String filePath = "C:\\Users\\12566\\Desktop\\大渡河数据\\径流资料\\历史资料\\水文数据\\";

        // 所有表格（每个文件代表一个数据类型）
        List<String> excelNames = List.of("日入库流量.xlsx", "日出库流量.xlsx", "日坝上水位.xlsx", "日坝下水位.xlsx", "日出力.xlsx", "日发电量.xlsx");

        for (String excelName : excelNames) {
            Object[][] data = ExcelUtils.readExcel(filePath + excelName, "Sheet1");
            if (data.length <= 1) continue;

            for (int j = 1; j < data.length; j++) {
                Object codeObj = data[j][0];
                Object timeObj = data[j][1];
                Object valueObj = data[j][3];

                if (!(codeObj instanceof Integer) || timeObj == null || !(valueObj instanceof Number)) continue;

                int code = (Integer) codeObj;
                Date time = (timeObj instanceof Date) ? (Date) timeObj : parseDate(timeObj.toString()); // 支持 String 日期
                double value = ((Number) valueObj).doubleValue();

                String fullName = splitMessages.get(String.valueOf(code));
                if (fullName == null) continue;

                String stationName = extractStationName(fullName);  // 修复了 stationName 含后缀的问题

                // 获取该电站的对应日期的数据对象
                tempMap.putIfAbsent(stationName, new HashMap<>());
                Map<Date, HisStationData> dateMap = tempMap.get(stationName);

                HisStationData dataItem = dateMap.computeIfAbsent(time, t -> {
                    HisStationData h = new HisStationData();
                    h.setStationName(stationName);
                    h.setTime(t);
                    return h;
                });

                // 按文件名设置字段
                switch (excelName) {
                    case "日入库流量.xlsx" -> dataItem.setInFlow(value);
                    case "日出库流量.xlsx" -> dataItem.setOutFlow(value);
                    case "日坝上水位.xlsx" -> dataItem.setWaterLevelUp(value);
                    case "日坝下水位.xlsx" -> dataItem.setWaterLevelDown(value);
                    case "日出力.xlsx" -> dataItem.setPowerOutput(value);
                    case "日发电量.xlsx" -> dataItem.setGeneration(value);
                }
            }
        }

// 将 tempMap 转为最终 stationData
        for (Map.Entry<String, Map<Date, HisStationData>> entry : tempMap.entrySet()) {
            List<HisStationData> dataList = new ArrayList<>(entry.getValue().values());
            dataList.sort(Comparator.comparing(HisStationData::getTime)); // 按时间排序
            stationData.put(entry.getKey(), dataList);
        }
        stationData.forEach((stationName, dataList) -> {
            Object[][] data = new Object[dataList.size()+1][8];
            data[0][0] = "电站";
            data[0][1] = "时间";
            data[0][2] = "入库径流";
            data[0][3] = "出库径流";
            data[0][4] = "坝上水位";
            data[0][5] = "坝下水位";
            data[0][6] = "出力";
            data[0][7] = "发电量";
            for (int i = 1; i < dataList.size(); i++) {
                data[i][0] = stationName;
                data[i][1] = dataList.get(i - 1).getTime();
                data[i][2] = dataList.get(i - 1).getInFlow();
                data[i][3] = dataList.get(i - 1).getOutFlow();
                data[i][4] = dataList.get(i - 1).getWaterLevelUp();
                data[i][5] = dataList.get(i - 1).getWaterLevelDown();
                data[i][6] = dataList.get(i - 1).getPowerOutput();
                data[i][7] = dataList.get(i - 1).getGeneration();
            }
            ExcelUtils.writeExcel("C:\\Users\\12566\\Desktop\\大渡河数据\\径流资料\\历史资料\\水文数据\\日尺度整合数据.xlsx", stationName, data);
        });
    }
    private static String extractStationName(String fullName) {
        if (fullName.contains("猴子岩"))
            return "猴子岩";
        if (fullName.contains("大岗山"))
            return "大岗山";
        if (fullName.contains("深溪沟"))
            return "深溪沟";
        if (fullName.contains("瀑布沟"))
            return "瀑布沟";
        if (fullName.contains("铜街子"))
            return "铜街子";
        if (fullName.contains("龚嘴"))
            return "龚嘴";
        if (fullName.contains("沙南"))
            return "沙南";
        if (fullName.contains("枕头坝"))
            return "枕头坝";
        else return fullName;
    }
}
