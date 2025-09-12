package pre_processing;

import com.hust.generatingcapacity.tools.ExcelUtils;

import java.util.ArrayList;
import java.util.List;

public class NHQPrecess {
    public static void main(String[] args) {
        Object[][] nhqData = ExcelUtils.readExcel("C:\\Users\\12566\\Desktop\\大渡河数据\\整合资料\\猴子岩工程参数.xlsx", "NHQ曲线");
        List<Object[]> nhqList = new ArrayList();
        for (int i = 1; i < nhqData.length; i++) {
            int l = 11;
            for (int j = 1; j < nhqData[i].length; j++) {
                if (nhqData[i][j] == null||nhqData[i][j].equals("")) {
                    l = j;
                    break;
                }
            }
            for (int j = 1; j < l; j++) {
                Object[] nq = new Object[3];
                nq[1] = nhqData[i][0];
                nq[0] = nhqData[0][j];
                nq[2] = nhqData[i][j];
                nhqList.add(nq);
            }
        }
        Object[][] result = nhqList.toArray(new Object[0][nhqList.size()]);
        ExcelUtils.writeExcel("C:\\Users\\12566\\Desktop\\大渡河数据\\整合资料\\猴子岩工程参数.xlsx", "NHQ曲线绘图版", result);
    }
}
