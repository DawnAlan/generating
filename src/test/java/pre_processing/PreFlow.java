package pre_processing;

import com.hust.generatingcapacity.tools.ExcelUtils;
import com.hust.generatingcapacity.tools.TimeUtils;
import lombok.SneakyThrows;

import java.text.SimpleDateFormat;
import java.util.*;

public class PreFlow {
    /**
     * 提取20枯水期预报径流数据
     */
//    public static void main(String[] args) throws ParseException {
//        String fileName = "D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\径流资料\\预报资料\\概率预报-日.xlsx";
//        Object[][] data1 = ExcelUtils.readExcel(fileName, "Sheet1");
//        Object[][] data2 = ExcelUtils.readExcel(fileName, "Sheet2");
//        Map<String, Integer> splitsPositions = Map.of("split1", 0, "time", 1);
//        Map<Integer, String> splitMessages = new TreeMap<>();
//        for (Object[] objects : data2) {
//            if (objects[0] instanceof Integer && objects[1] instanceof String) {
//                splitMessages.put((Integer) objects[0], (String) objects[1]);
//            }
//        }
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
//        TimeRange timeRange = new TimeRange(sdf.parse("2024-11-01 00"), sdf.parse("2025-04-30 23"));
//        List<Integer> splits = new ArrayList<>();
//        splits.add(15000016);
//        splits.add(15000007);
//        splits.add(15000002);
//        splits.add(15000001);
////        splits.add(30000005);
////        splits.add(30000025);
////        splits.add(30000027);
////        splits.add(30000028);
//        String fileName2 = "D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\径流资料\\预报资料\\24年枯期预报数据\\大渡河24年枯期概率预报-日.xlsx";
//        for (Integer split : splits) {
//            Object[][] result = PreProcessUtils.splitData(data1, splitsPositions, timeRange, splitMessages, split);
//            ExcelUtils.writeExcel(fileName2, splitMessages.get(split), result);
//            StringBuilder sb = new StringBuilder();
//            sb.append("已将数据按照筛选条件拆分，生成新的sheet，sheet名称为：").append(splitMessages.get(split)).append("\n");
//            if (!timeRange.isEmpty()) {
//                sb.append("时间范围：").append(timeRange.getStart()).append(" - ").append(timeRange.getEnd()).append("\n");
//            }
//            sb.append("数据量：").append(result.length - 1).append("\n");
//            System.out.println(sb);
//        }
//    }
    @SneakyThrows
    public static void main(String[] args) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fileName = "D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\径流资料\\预报资料\\20年枯期预报数据\\大渡河20年枯期概率预报-日.xlsx";
        String resultName = "D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\径流资料\\预报资料\\20年枯期预报数据\\大渡河20年枯期不同预见期概率预报-日.xlsx";
        List<String> sheets = ExcelUtils.checkSheetsInExcel(fileName);
        Integer[] preDays = new Integer[]{1, 3, 10};
        Date start = sdf.parse("2020-10-31");
        Date end = sdf.parse("2021-04-30");

//        Object[] head = new Object[5];
//        head[0] = "电站";
//        head[1] = "日期";
//        head[2] = "预见期1天";
//        head[3] = "预见期3天";
//        head[4] = "预见期10天";
//        result.put(sdf.parse("2002-01-28"),head);
        for (int i = 0; i < sheets.size(); i++) {
            Object[][] data = ExcelUtils.readExcel(fileName, sheets.get(i));
            Map<String, List<Object>> result = new LinkedHashMap<>();

            for (int j = 0; j < preDays.length; j++) {
                List<Object> row = new ArrayList<>();
                List<Object> time = new ArrayList<>();
                boolean matched = false;
                int dateIndex = 1;
                for (int k = 1; k < data.length; k++) {
                    Date dateP = (Date) data[k][1];//相似性与概率需要修改
                    Date date = TimeUtils.addCalendar(start, "日", dateIndex);
                    if (matched && TimeUtils.dateCompare(date, dateP, "日")) {
                        continue;
                    }
                    if (!TimeUtils.dateCompare(date, dateP, "日")) {
                        matched = false;
                    }
                    if ((dateP).after(date)) {
                        int blank = TimeUtils.getDateDuration(date, dateP, "日");
                        for (int l = 0; l < blank; l++) {
                            time.add(TimeUtils.addCalendar(date, "日", l));
                            row.add(0.0);
                        }
                        dateIndex += blank;
                        date = dateP;
                    }
                    if (TimeUtils.dateCompare(date, dateP, "日")) {
                        for (int l = 0; l < preDays[j]; l++) {
                            time.add(data[k + l][2]);//相似性与概率需要修改
                            row.add(data[k + l][3]);//相似性与概率需要修改
                        }
                        dateIndex += preDays[j];
                        matched = true;
                    }
                }
                result.put("日期", time);
                result.put("预见期" + preDays[j] + "天", row);
            }
            Object[][] resultObject = new Object[result.get("预见期1天").size() + 1][4];
            resultObject[0][0] = "日期";
            resultObject[0][1] = "预见期1天";
            resultObject[0][2] = "预见期3天";
            resultObject[0][3] = "预见期10天";
            for (int j = 0; j < result.get("预见期1天").size(); j++) {
                resultObject[j + 1][0] = result.get("日期").get(j);
                resultObject[j + 1][1] = result.get("预见期1天").get(j);
                resultObject[j + 1][2] = result.get("预见期3天").get(j);
                resultObject[j + 1][3] = result.get("预见期10天").get(j);
            }
            ExcelUtils.writeExcel(resultName, sheets.get(i), resultObject);
        }
    }

}
