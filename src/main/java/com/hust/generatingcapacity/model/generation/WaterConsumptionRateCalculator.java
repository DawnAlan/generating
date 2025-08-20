package com.hust.generatingcapacity.model.generation;


import com.hust.generatingcapacity.entity.CalculateData;
import com.hust.generatingcapacity.entity.CodeValue;
import com.hust.generatingcapacity.entity.HNQData;
import com.hust.generatingcapacity.entity.NHQCell;
import com.hust.generatingcapacity.tools.ExcelUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.hust.generatingcapacity.tools.Tools.changeObjToDouble;

public class WaterConsumptionRateCalculator {
    public static List<HNQData> hnqList = new ArrayList<>();

    public static void main(String[] args) {
        //获取水位库容曲线
        Object[][] levelCap = ExcelUtils.readExcel("C:\\Users\\12566\\Desktop\\大渡河数据\\整合资料\\猴子岩工程参数.xlsx", "水位库容曲线");
        List<CodeValue> levelCapList = new ArrayList<>();
        for (int i = 1; i < levelCap.length; i++) {
            CodeValue codeValue = new CodeValue(Double.parseDouble(levelCap[i][0].toString()), Double.parseDouble(levelCap[i][1].toString()));
            levelCapList.add(codeValue);
        }
        //获取水位耗水率曲线
        Object[][] consumptionRate = ExcelUtils.readExcel("C:\\Users\\12566\\Desktop\\大渡河数据\\整合资料\\猴子岩工程参数.xlsx", "耗水率曲线");
        List<CodeValue> consumptionRateList = new ArrayList<>();
        for (int i = 1; i < consumptionRate.length; i++) {
            CodeValue codeValue = new CodeValue(Double.parseDouble(consumptionRate[i][0].toString()), Double.parseDouble(consumptionRate[i][1].toString()));
            consumptionRateList.add(codeValue);
        }
        //获取NHQ曲线
        Object[][] NHQData = ExcelUtils.readExcel("C:\\Users\\12566\\Desktop\\大渡河数据\\整合资料\\猴子岩工程参数.xlsx", "NHQ曲线");
        List<NHQCell> NHQDataList = new ArrayList<>();
        for (int i = 1; i < NHQData.length; i++) {
            for (int j = 1; j < 11; j++) {
                NHQCell cell = new NHQCell();
                if (NHQData[i][j] != null && !NHQData[i][j].equals("")) {
                    cell.setH(Double.parseDouble(NHQData[i][0].toString()));
                    cell.setQ(Double.parseDouble(NHQData[i][j].toString()));
                    cell.setN(Double.parseDouble(NHQData[0][j].toString()));
                    NHQDataList.add(cell);
                }
            }
        }
        //可以获取到插值的出力
        hnqList = NHQCell.convert("N", NHQDataList);
        //获取径流、水位、水头数据
        Object[][] historyData = ExcelUtils.readExcel("C:\\Users\\12566\\Desktop\\大渡河数据\\整合资料\\猴子岩24年汛期数据.xlsx", "历史真实值");
        //分为以下几种情况：1.根据NHQ计算，由历史真实径流和水位变化计算出库，然后计算出力，计算发电能力（计算值和真实值对比）
        //2.根据耗水率曲线计算，由历史真实径流和水位变化计算出库，出库与满发流量取小，计算发电能力
        List<CalculateData> historyDataList = new ArrayList<>();
        for (int i = 1; i < historyData.length - 1; i++) {
            CalculateData calculateData = new CalculateData();
            calculateData.setStation(historyData[i][0].toString());
            calculateData.setTime((Date) historyData[i][1]);
            calculateData.setInFlow(changeObjToDouble(historyData[i][2]));
            calculateData.setLevelBef(changeObjToDouble(historyData[i][3]));
            calculateData.setLevelAft(changeObjToDouble(historyData[i + 1][3]));
            calculateData.setHead(changeObjToDouble(historyData[i][3]) - changeObjToDouble(historyData[i][4]));
            calculateData.setUnits(4);
            historyDataList.add(calculateData);
        }
//        Object[][] result = new Object[historyData.length][6];
//        result[0][0] = "水电站";
//        result[0][1] = "日期";
//        result[0][2] = "入库径流";
//        result[0][3] = "坝上水位";
//        result[0][4] = "水头";
//        result[0][5] = "NHQ计算发电量";
//        for (int i = 0; i < historyDataList.size(); i++) {
//            calculateByNHQ(historyDataList.get(i), levelCapList, hnqList);
//            result[i + 1][0] = historyDataList.get(i).getStation();
//            result[i + 1][1] = historyDataList.get(i).getTime();
//            result[i + 1][2] = historyDataList.get(i).getInFlow();
//            result[i + 1][3] = historyDataList.get(i).getLevelBef();
//            result[i + 1][4] = historyDataList.get(i).getHead();
//            result[i + 1][5] = historyDataList.get(i).getCalGen();
//        }
//        ExcelUtils.writeExcel("C:\\Users\\12566\\Desktop\\大渡河数据\\整合资料\\猴子岩24年汛期发电计算.xlsx", "历史真实值-NHQ", result);
        Object[][] result1 = new Object[historyData.length][6];
        result1[0][0] = "水电站";
        result1[0][1] = "日期";
        result1[0][2] = "入库径流";
        result1[0][3] = "坝上水位";
        result1[0][4] = "水头";
        result1[0][5] = "耗水率计算发电量";
        for (int i = 0; i < historyDataList.size(); i++) {
            calculateByConsumptionRate(historyDataList.get(i), levelCapList, consumptionRateList);
            result1[i + 1][0] = historyDataList.get(i).getStation();
            result1[i + 1][1] = historyDataList.get(i).getTime();
            result1[i + 1][2] = historyDataList.get(i).getInFlow();
            result1[i + 1][3] = historyDataList.get(i).getLevelBef();
            result1[i + 1][4] = historyDataList.get(i).getHead();
            result1[i + 1][5] = historyDataList.get(i).getCalGen();
        }
        ExcelUtils.writeExcel("C:\\Users\\12566\\Desktop\\大渡河数据\\整合资料\\猴子岩24年汛期发电计算.xlsx", "历史真实值-耗水率", result1);

        //3.根据NHQ计算，由不同预见期的径流和水位变化计算出库，然后计算出力，再计算发电能力（最大和最小就在水位的变化上）
        //4.根据耗水率曲线计算，由不同预见期径流和水位变化计算出库，出库与满发流量取小，计算发电能力（最大和最小就在水位的变化上）


    }

    /**
     * @param calData
     * @param levelCapList
     * @param hnqList      此时Value为 N
     * @return
     */
    public static void calculateByNHQ(CalculateData calData, List<CodeValue> levelCapList, List<HNQData> hnqList) {
        //库容变化量
        double capChange = Math.pow(10, 6) * (CodeValue.linearInterpolation(calData.getLevelAft(), levelCapList) - CodeValue.linearInterpolation(calData.getLevelBef(), levelCapList));
        //来水量
        double inQ = calData.getInFlow() * 24 * 3600;
        //出库水量
        double outQ = Math.max(0, inQ - capChange);
        if (outQ == 0) {
            System.out.println("请注意， " + calData.getTime() + " 日期下的出库流量计算值为0！");
        }
        //日均出库流量
        double outFlow = outQ / 24 / 3600;
        //获取该水头下的单个机组最大发电流量
        double maxOneCalFlow = HNQData.getMaxCode(calData.getHead(), hnqList);
        //获取满发机组数量和各机组负荷
        int maxUnits = Math.min(calData.getUnits(), (int) (outFlow / maxOneCalFlow));
        //计算机组总负荷
        double allN = maxUnits * HNQData.lineInterpolation(calData.getHead(), maxOneCalFlow, hnqList);
        if (maxUnits < calData.getUnits()) {//存在机组未满发
            double oneCalFlow = outFlow - maxOneCalFlow * maxUnits;
            double oneCalN = HNQData.lineInterpolation(calData.getHead(), oneCalFlow, hnqList);
            allN += oneCalN;
        }
        calData.setCalGen(allN * 24);
//        return allN * 24;//(WM*h)
    }


    /**
     * @param calData
     * @param levelCapList
     * @param consumptionRateList
     * @return
     */
    public static void calculateByConsumptionRate(CalculateData calData, List<CodeValue> levelCapList, List<CodeValue> consumptionRateList) {
        //库容变化量
        double capChange = Math.pow(10, 6) * (CodeValue.linearInterpolation(calData.getLevelAft(), levelCapList) - CodeValue.linearInterpolation(calData.getLevelBef(), levelCapList));
        //来水量
        double inQ = calData.getInFlow() * 24 * 3600;
        //出库水量
        double outQ = Math.max(0, inQ - capChange);
        if (outQ == 0) {
            System.out.println("请注意， " + calData.getTime() + " 日期下的出库流量计算值为0！");
        }
        //日均出库流量
        double outFlow = outQ / 24 / 3600;
        //获取该水头下的单个机组最大发电流量
        double maxOneCalFlow = HNQData.getMaxCode(calData.getHead(), hnqList);
        //获取最大发电流量
        double maxCalFlow = Math.min(maxOneCalFlow * calData.getUnits(), outFlow);
//        double maxCalFlow = outFlow;
        double maxCalQ = maxCalFlow * 24 * 3600;
        double rate = CodeValue.linearInterpolation(calData.getLevelBef(), consumptionRateList);
        calData.setCalGen(maxCalQ / rate / Math.pow(10, 3));
//        return maxCalQ / rate / Math.pow(10, 3);//(WM*h)
    }


}
