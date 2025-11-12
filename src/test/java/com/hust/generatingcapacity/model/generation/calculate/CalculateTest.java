package com.hust.generatingcapacity.model.generation.calculate;

import com.hust.generatingcapacity.GeneratingCapacityApplication;
import com.hust.generatingcapacity.iservice.IHydropowerStationService;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import com.hust.generatingcapacity.model.generation.vo.*;
import com.hust.generatingcapacity.tools.ExcelUtils;
import com.hust.generatingcapacity.tools.TimeUtils;
import com.hust.generatingcapacity.tools.Tools;
import org.junit.jupiter.api.Test;
import org.mapstruct.ap.internal.gem.GemGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@SpringBootTest(classes = GeneratingCapacityApplication.class)
public class CalculateTest {
    @Autowired
    private IHydropowerStationService hydropowerStationService;

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Test
    public void testOneStationCal() throws ParseException {
        String stationName = "猴子岩";
        int L = 1;
        String period = "日";
        Date start = sdf.parse("2020-11-09");
        Date end = sdf.parse("2020-11-10");
        StationData stationData = hydropowerStationService.changeToStationData(hydropowerStationService.get(stationName));
        Object[][] data = ExcelUtils.readExcel("D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\发电计算\\枯期测试-大渡河\\管辖内水电站20年枯期日尺度整合数据.xlsx", stationName);
        CalculateParam param = setCalculateParam(stationName, stationData, L, true, period);
        int length = TimeUtils.getDateDuration(start, end, "日") / L;
        List<CalculateStep> result = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            CalculateInput input = new CalculateInput();
            input.setStation(stationName);
            input.setStart(TimeUtils.addCalendar(start, period, i * L));
            input.setPeriod(period);
            input.setL(L);
            getCalInputFromWithinExcel(data, input);
            input.checkForecast();
            CalculateStep step = new CalculateStep(input);
            CalculateVO vo = new CalculateVO(step, input, param, stationData);
            List<CalculateStep> res = CalculateProcess.LStepCalculate(vo);
            result.addAll(res);
        }
        Object[][] res = new Object[result.size() + 1][];
        res[0] = new Object[]{"时间", "是否修正", "入库径流", "开始水位", "结束水位", "发电流量", "出库流量", "规程计算结果", "历史真实发电（MW*H）", "警告信息"};
        for (int i = 0; i < result.size(); i++) {
            int finalI = i;
            Object value = Arrays.stream(data)
                    .skip(1)
                    .filter(d -> TimeUtils.dateCompare((Date) ((Object[]) d)[1], result.get(finalI).getTime(), period))
                    .findFirst()   // 返回 Optional<Object>
                    .map(d -> ((Object[]) d)[7])  // 取下标
                    .orElse(null); // 如果没找到，返回 null
            res[i + 1] = new Object[]{
                    result.get(i).getTime(), result.get(i).isRevise(), result.get(i).getInFlow(),
                    result.get(i).getLevelBef(), result.get(i).getLevelAft(),
                    result.get(i).getQp(), result.get(i).getQo(),
                    result.get(i).getCalGen(), Tools.changeObjToDouble(value) * 10, result.get(i).getRemark()
            };
        }
        ExcelUtils.writeExcel("D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\发电计算\\枯期测试-大渡河\\输出\\20年枯期日尺度最小发电能力计算结果-" + stationName + ".xlsx", stationName, res);

    }


    @Test
    public void testAllStationCal() throws ParseException {
        List<String> stations = List.of("猴子岩", "长河坝", "黄金坪", "泸定", "大岗山",
                "龙头石", "瀑布沟", "深溪沟", "枕头坝一级", "沙坪二级",
                "龚嘴", "铜街子", "沙湾", "安谷");
//        List<String> stations = List.of( "安谷");
        int L = 1;
        String period = "日";
        Date start = sdf.parse("2020-11-01");
//        Date end = sdf.parse("2020-11-03");
        Date end = sdf.parse("2021-04-30");
        allStationCal(stations, start, end, period, L, false);
        allStationCal(stations, start, end, period, L, true);
    }

    private void allStationCal(List<String> stations, Date start, Date end, String period, int L, boolean isGenMin) {
        Map<String, StationData> stationDataMap = new HashMap<>();
        Map<String, Object[][]> dataMap = new HashMap<>();
        for (String stationName : stations) {
            StationData stationData = hydropowerStationService.changeToStationData(hydropowerStationService.get(stationName));
            Object[][] data = ExcelUtils.readExcel("D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\发电计算\\枯期测试-大渡河\\大渡河水电站20年枯期日尺度整合数据.xlsx", stationName);
            stationDataMap.put(stationName, stationData);
            dataMap.put(stationName, data);
        }
        int length = TimeUtils.getDateDuration(start, end, "日") / L;
        Map<String, List<CalculateStep>> result = new HashMap<>();
        for (int i = 0; i < length; i++) {
            Map<String, CalculateVO> map = new LinkedHashMap<>();
            for (String stationName : stations) {
                CalculateParam calParam = setCalculateParam(stationName, stationDataMap.get(stationName), L, isGenMin, period);
                CalculateInput input = setCalculateInput(stationName, TimeUtils.addCalendar(start, period, i * L), dataMap.get(stationName), L, period);
                CalculateStep step = new CalculateStep(input);
                CalculateVO vo = new CalculateVO(step, input, calParam, stationDataMap.get(stationName));
                map.put(stationName, vo);
            }
            // 当前时段所有电站的计算结果
            Map<String, List<CalculateStep>> current = CalculateProcess.LStepAllStationCalculate(map);
            // 累积到 result
            for (Map.Entry<String, List<CalculateStep>> entry : current.entrySet()) {
                String station = entry.getKey();
                List<CalculateStep> steps = entry.getValue();
                result.computeIfAbsent(station, k -> new ArrayList<>()).addAll(steps);
                System.out.println(station + " 第" + i + " 时段计算完成………………");
            }
        }
        // 输出结果
        for (String station : stations) {
            List<CalculateStep> steps = result.get(station);
            Object[][] res = new Object[steps.size() + 1][];
            res[0] = new Object[]{"时间", "是否修正", "入库径流", "开始水位", "结束水位", "发电流量", "出库流量", "规程计算结果", "历史真实发电（MW*H）", "警告信息"};
            for (int i = 0; i < steps.size(); i++) {
                int finalI = i;
                Object value = Arrays.stream(dataMap.get(station))
                        .skip(1)
                        .filter(d -> TimeUtils.dateCompare((Date) ((Object[]) d)[0], steps.get(finalI).getTime(), period))
                        .findFirst()   // 返回 Optional<Object>
                        .map(d -> ((Object[]) d)[5])  // 取下标
                        .orElse(null); // 如果没找到，返回 null
                res[i + 1] = new Object[]{
                        steps.get(i).getTime(), steps.get(i).isRevise(), steps.get(i).getInFlow(),
                        steps.get(i).getLevelBef(), steps.get(i).getLevelAft(),
                        steps.get(i).getQp(), steps.get(i).getQo(),
                        steps.get(i).getCalGen(), value, steps.get(i).getRemark()
                };
            }
            if (!isGenMin) {
                ExcelUtils.writeExcel("D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\发电计算\\枯期测试-大渡河\\输出\\20年枯期日尺度最大发电能力计算结果" + "-预见期" + L + "天.xlsx", station, res);
            } else {
                ExcelUtils.writeExcel("D:\\Data\\5.大渡河\\整理数据\\大渡河流域内部发电能力预测\\发电计算\\枯期测试-大渡河\\输出\\20年枯期日尺度最小发电能力计算结果" + "-预见期" + L + "天.xlsx", station, res);
            }

        }

    }

    private static CalculateInput setCalculateInput(String stationName, Date date, Object[][] data, int L, String period) {
        CalculateInput input = new CalculateInput();
        input.setStation(stationName);
        input.setStart(date);
        input.setPeriod(period);
        input.setL(L);
        getCalInputFromExcel(data, input);
        input.checkForecast();
        return input;
    }

    private static CalculateParam setCalculateParam(String stationName, StationData stationData, int L, Boolean isGenMin, String period) {
        CalculateParam param = new CalculateParam();
        param.setStation(stationName);
        param.setPeriod(CalculateInput.changePeriod(period));
        param.setL(L);
        param.setConsiderH(stationData.getIsUnderDdh() && !stationData.getNHQLines().isEmpty());
        param.setGenMin(isGenMin);
        param.setIntervalFlow(!stationName.equals("猴子岩"));//需要修改
        return param;
    }

    private static void getCalInputFromExcel(Object[][] data, CalculateInput input) {
        Object[][] filterData = Arrays.stream(data)
                .skip(1)
                .filter(d -> TimeUtils.isAfterOrSame((Date) d[0], input.getStart(), input.getPeriod()))
                .limit(input.getL())
                .toArray(Object[][]::new);
        input.setWaterLevel(Tools.changeObjToDouble(filterData[0][3]));
        input.setTailLevel(Tools.changeObjToDouble(filterData[0][4]));
        input.setFinalLevel(Tools.changeObjToDouble(filterData[filterData.length - 1][3]));
        input.setInFlows(Arrays.stream(filterData).map(d -> new PreFlow((Date) d[0], Tools.changeObjToDouble(d[1]))).toList());
    }

    /**
     * 管辖内的数据结构
     *
     * @param data
     * @param input
     */
    private static void getCalInputFromWithinExcel(Object[][] data, CalculateInput input) {
        Object[][] filterData = Arrays.stream(data)
                .skip(1)
                .filter(d -> TimeUtils.isAfterOrSame((Date) d[1], input.getStart(), input.getPeriod()))
                .limit(input.getL())
                .toArray(Object[][]::new);
        input.setWaterLevel(Tools.changeObjToDouble(filterData[0][4]));
        input.setTailLevel(Tools.changeObjToDouble(filterData[0][5]));
        input.setFinalLevel(Tools.changeObjToDouble(filterData[filterData.length - 1][4]));
        input.setInFlows(Arrays.stream(filterData).map(d -> new PreFlow((Date) d[1], Tools.changeObjToDouble(d[2]))).toList());
    }


}
