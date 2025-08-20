package com.hust.generatingcapacity.tools;


/**
 * 通用数学方法
 *
 * @author leileilei
 */

public class MathUtils {

    /**
     * 最值归一化
     *
     * @param data      要归一化的数据
     * @param maxAndMin 数据的最大最小值
     * @return
     */
    public static double[] normalization(double[] data, double[] maxAndMin) {
        double[] normalData = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            normalData[i] = (data[i] - maxAndMin[1]) / (maxAndMin[0] - maxAndMin[1]);
        }
        return normalData;
    }

    /**
     * 反归一化
     *
     * @param data      要反归一化的数据
     * @param maxAndMin 数据的最大最小值
     * @return
     */
    public static double[] reNormal(double[] data, double[] maxAndMin) {
        double[] renormalData = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            renormalData[i] = data[i] * (maxAndMin[0] - maxAndMin[1]) + maxAndMin[1];
        }
        return renormalData;
    }

    /**
     * 找到最大值
     *
     * @param data
     * @return
     */
    public static double[] findMax(double[] data) {
        double[] maxAndMin = new double[2];
        for (int i = 0; i < data.length; i++) {
            if (data[i] > maxAndMin[0]) {
                maxAndMin[0] = data[i];
            }
        }
        maxAndMin[1] = Double.MAX_VALUE;
        for (int i = 0; i < data.length; i++) {
            if (data[i] < maxAndMin[1]) {
                maxAndMin[1] = data[i];
            }
        }

        return maxAndMin;
    }

    /**
     * 最值归一化
     *
     * @param data      要归一化的数据
     * @param maxAndMin 数据的最大最小值,第一维表示最大值，第二维表示最小值
     * @return
     */
    public static double[][] normalization(double[][] data, double[][] maxAndMin) {
        double[][] normalData = new double[data.length][data[0].length];
        for (int j = 0; j < data[0].length; j++) {
            for (int i = 0; i < data.length; i++) {
                normalData[i][j] = (data[i][j] - maxAndMin[1][j]) / (maxAndMin[0][j] - maxAndMin[1][j]);
            }
        }
        return normalData;
    }

    /**
     * 反归一化
     *
     * @param data      要反归一化的数据
     * @param maxAndMin 数据的最大最小值,第一维表示最大值，第二维表示最小值
     * @return
     */
    public static double[][] reNormal(double[][] data, double[][] maxAndMin) {
        double[][] renormalData = new double[data.length][data[0].length];
        for (int j = 0; j < data[0].length; j++) {
            for (int i = 0; i < data.length; i++) {
                renormalData[i][j] = data[i][j] * (maxAndMin[0][j] - maxAndMin[1][j]) + maxAndMin[1][j];
            }
        }
        return renormalData;
    }

    /**
     * 找到最大值
     *
     * @param data
     * @return 第一行最大值，第二行最小值
     */
    public static double[][] findMaxAndMin(double[][] data) {
        double[][] maxAndMin = new double[2][data[0].length];
        for (int j = 0; j < data[0].length; j++) {
            maxAndMin[0][j] = -Double.MAX_VALUE;
            for (int i = 0; i < data.length; i++) {
                if (data[i][j] > maxAndMin[0][j]) {
                    maxAndMin[0][j] = data[i][j];//最大值
                }
            }
        }

        for (int j = 0; j < data[0].length; j++) {
            maxAndMin[1][j] = Double.MAX_VALUE;
            for (int i = 0; i < data.length; i++) {
                if (data[i][j] < maxAndMin[1][j]) {
                    maxAndMin[1][j] = data[i][j];//最小值
                }
            }
        }
        return maxAndMin;
    }

    // 返回核函数值
    public static double k_function(double[] x, double[] y, double gamma) {

        double sum = 0;
        int xlen = x.length;

        for (int i = 0; i < xlen; i++) {
            double d = x[i] - y[i];
            sum += d * d;
        }

        return Math.exp(-gamma * sum);
    }

    public static double RMSE(double[][] a, double[][] b) {
        if (a.length != b.length || a[0].length != b[0].length) {

            return -1;
        }
        double[] result = new double[a[0].length];
        for (int i = 0; i < a[0].length; i++) {
            for (int j = 0; j < a.length; j++) {
                result[i] += Math.pow(a[j][i] - b[j][i], 2);
            }
            result[i] = Math.pow(result[i] / a.length, 0.5);
        }

        double avgResult = 0;
        for (int i = 0; i < a[0].length; i++) {
            avgResult += result[i] / a[0].length;
        }
        return avgResult;
    }

    public static double RMSE(double[] a, double[] b) {
        if (a.length != b.length) {

            return -1;
        }
        double result = 0;
        for (int i = 0; i < a.length; i++) {
            result += Math.pow(a[i] - b[i], 2);
        }
        result = Math.pow(result / a.length, 0.5);

        return result;
    }

    // 确定性系数
    public static double nashCoefDouble(double[] foreFlowWhole, double[] runoffWhole) {
        double DC = 0;
        double a = 0;
        double b = 0;
        double c = 0;

        for (int i = 0; i < runoffWhole.length; i++) {
            a += runoffWhole[i];
        }
        a = a / runoffWhole.length;

        for (int i = 0; i < runoffWhole.length; i++) {
            b += (runoffWhole[i] - a) * (runoffWhole[i] - a);
        }

        for (int i = 0; i < runoffWhole.length; i++) {
            c += (runoffWhole[i] - foreFlowWhole[i]) * (runoffWhole[i] - foreFlowWhole[i]);
        }

        DC = 1 - c / b;
        return DC;
    }

    // 洪量指标
    public static double FloodAmountRate(double[] foreFlow_plusRunoff3hour, double[] runoff3hour) {
        double foreflowSum = 0;
        double runoffSum = 0;
        for (int j = 0; j < foreFlow_plusRunoff3hour.length; j++) {
            foreflowSum += foreFlow_plusRunoff3hour[j];
            runoffSum += runoff3hour[j];
        }
        double qulifiedRate = Math.abs(runoffSum - foreflowSum) / runoffSum;
        return qulifiedRate;
    }

    // 找到最大值
    public static double Maxget(double[] q) {
        double max = 0;
        for (int i = 0; i < q.length; i++) {
            if (q[i] > max) {
                max = q[i];
            }
        }

        return max;
    }

    public static double FloodPeakRate(double[] foreFlow, double[] runoff) {

        double foreflowMax = Maxget(foreFlow);
        double runoffMax = Maxget(runoff);
        double QmaxTotal = (Math.abs(foreflowMax - runoffMax)) / runoffMax;
        return QmaxTotal;
    }

    public static double MRE(double[][] a, double[][] b) {
        if (a.length != b.length || a[0].length != b[0].length) {
            return -1;
        }
        double[] result = new double[a[0].length];
        int size = a.length;
        for (int i = 0; i < a[0].length; i++) {
            for (int j = 0; j < a.length; j++) {
                if (a[j][i] <= 0) {
                    size--;
                    continue;
                }
                result[i] += Math.abs(a[j][i] - b[j][i]) / a[j][i];
            }
            result[i] = result[i] / size;
        }
        double avgResult = 0;
        for (int i = 0; i < a[0].length; i++) {
            avgResult += result[i] / a[0].length;
        }
        return avgResult;
    }

    // 计算确定性系数
    public static double R2(double[][] real, double[][] estimate) {
        double[] dc = new double[real[0].length];
        for (int i = 0; i < dc.length; i++) {
            double a = 0;
            double b = 0;
            double c = 0;

            for (int j = 0; j < real.length; j++) {
                a += real[j][i];
            }
            a = a / real.length;

            for (int j = 0; j < real.length; j++) {
                b += (real[j][i] - a) * (real[j][i] - a);
            }

            for (int j = 0; j < real.length; j++) {
                c += (real[j][i] - estimate[j][i]) * (real[j][i] - estimate[j][i]);
            }
            dc[i] = 1 - c / b;
        }

        double avgResult = 0;
        for (int i = 0; i < real[0].length; i++) {
            avgResult += dc[i] / real[0].length;
        }
        return avgResult;
    }

    // 计算合格率
    public static double QualifyRate(double[][] real, double[][] estimate) {
        int size = real.length;
        int[] qualifyNum = new int[real.length];
        double[] qr = new double[real.length];
        for (int j = 0; j < real.length; j++) {
            for (int i = 0; i < real[0].length; i++) {
                qualifyNum[j] = 0;
                if (Math.abs(estimate[j][i] - real[j][i]) / real[j][i] <= 0.2) {
                    qualifyNum[j]++;
                }
            }
        }
        double sum = 0;
        for (int i = 0; i < qr.length; i++) {
            qr[i] = (double) qualifyNum[i] / size;
            sum += qr[i];
        }

        double avgResult = 0;
        for (int i = 0; i < real.length; i++) {
            avgResult += qr[i] / real[0].length;
        }
        return avgResult;
    }

    // 计算合格率
    public static double QualifyRateLarge(double[][] real, double[][] estimate) {
        int size = real.length;
        int[] qualifyNum = new int[real.length];
        double[] qr = new double[real.length];
        for (int j = 0; j < real.length; j++) {
            for (int i = 0; i < real[0].length; i++) {
                qualifyNum[j] = 0;
                if (estimate[j][i] > real[j][i]) {
                    qualifyNum[j]++;
                }
            }
        }
        double sum = 0;
        for (int i = 0; i < qr.length; i++) {
            qr[i] = (double) qualifyNum[i] / size;
            sum += qr[i];
        }

        double avgResult = 0;
        for (int i = 0; i < real.length; i++) {
            avgResult += qr[i] / real[0].length;
        }
        return avgResult;
    }

    // 计算合格率
    public static double QualifyRateTen(double[][] real, double[][] estimate) {
        int size = real.length;
        int[] qualifyNum = new int[real.length];
        double[] qr = new double[real.length];
        for (int j = 0; j < real.length; j++) {
            for (int i = 0; i < real[0].length; i++) {
                qualifyNum[j] = 0;
                if (Math.abs(estimate[j][i] - real[j][i]) / real[j][i] <= 0.1) {
                    qualifyNum[j]++;
                }
            }
        }
        double sum = 0;
        for (int i = 0; i < qr.length; i++) {
            qr[i] = (double) qualifyNum[i] / size;
            sum += qr[i];
        }

        double avgResult = 0;
        for (int i = 0; i < real.length; i++) {
            avgResult += qr[i] / real[0].length;
        }
        return avgResult;
    }

    // 计算合格率
    public static double ChangeQualify(double[][] real, double[][] estimate) {
        int size = real.length;
        int[] qualifyNum = new int[real.length];
        double[] qr = new double[real.length];
        double[][] extremun = findMaxAndMin(real);
        double change = extremun[0][0] - extremun[1][0];
        double zhibiao = Math.min(change * 0.2, 12.5);
        for (int j = 0; j < real.length; j++) {
            for (int i = 0; i < real[0].length; i++) {
                qualifyNum[j] = 0;
                if (Math.abs(estimate[j][i] - real[j][i]) <= Math.min(zhibiao, real[j][i] * 0.2)) {
                    qualifyNum[j]++;
                }
            }
        }
        double sum = 0;
        for (int i = 0; i < qr.length; i++) {
            qr[i] = (double) qualifyNum[i] / size;
            sum += qr[i];
        }

        double avgResult = 0;
        for (int i = 0; i < real.length; i++) {
            avgResult += qr[i] / real[0].length;
        }
        return avgResult;
    }

    public static String Rank(double dc) {
        String rank = new String();
        if (dc > 0.9) {
            rank = "甲等";
        } else if (dc <= 0.9 && (dc >= 0.7)) {
            rank = "乙等";
        } else {
            rank = "丙等";
        }
        return rank;
    }

    public static double GuassionFunc(double variable, double sigm) {
        return Math.exp(-Math.pow(variable / sigm, 2) / 2);
    }

    public static double SoftPlus(double input) {
        return Math.log(1 + Math.pow(Math.E, input));
    }

    public static double Derivate_SoftPlust(double input) {
        return Sigmod(input);
    }

    public static double Sigmod(double input) {
        return 1 / (1 + Math.pow(Math.E, input));
    }

    public static double Relu(double input) {
        return Math.max(0, input);
    }

    public static double Derivate_Relu(double input) {
        int value = 0;
        if (input > 0) {
            value = 1;
        } else {
            value = 0;
        }
        return value;
    }

    public static double LeakyRelu(double input) {
        double value = 0;
        if (input > 0) {
            value = input;
        } else {
            value = 0.5 * input;
        }

        return value;
    }

    public static double Derivate_LeakyRelu(double input) {
        double value = 0;
        if (input > 0) {
            value = 1;
        } else {
            value = 0.5;
        }

        return value;
    }
}
