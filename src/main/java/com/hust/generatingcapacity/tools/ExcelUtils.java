package com.hust.generatingcapacity.tools;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class ExcelUtils {


    public static Object[][] readExcel(String fileName, String sheetName) {
        ZipSecureFile.setMinInflateRatio(-1.0d);
        try (InputStream is = getInputStreamSmart(fileName)) {
            Workbook workbook;
            if (is != null) {
                workbook = WorkbookFactory.create(is);
            } else {
                throw new RuntimeException("无法读取Excel文件，请检查文件路径和格式是否正确。");
            }
            if (workbook == null) {
                throw new RuntimeException("无法读取Excel文件或工作簿为空，请检查文件路径和格式是否正确。");
            }
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException("工作表 " + sheetName + " 不存在，请检查工作表名称是否正确。");
            }
            // 获取最大行数和列数
            int maxRow = sheet.getLastRowNum() + 1;
            int maxCol = sheet.getRow(0).getLastCellNum();
            Object[][] data = readSheet(sheet, maxRow, maxCol);
            workbook.close();
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object[][] readSheet(Sheet sheet, int maxRow, int maxCol) {
        // 创建二维Object数组
        Object[][] data = new Object[maxRow][maxCol];
        // 遍历每一行
        for (int rowIndex = 0; rowIndex < maxRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue; // 跳过空行

            // 遍历每一列
            for (int colIndex = 0; colIndex < maxCol; colIndex++) {
                Cell cell = row.getCell(colIndex);
                if (cell == null) {
                    data[rowIndex][colIndex] = null;
                    continue;
                }

                // 判断该单元格是否是合并单元格的一部分
                if (isMergedCell(sheet, rowIndex, colIndex)) {
                    // 如果该单元格是合并单元格的第二部分或之后的部分，则设为null
                    data[rowIndex][colIndex] = null;
                    continue;
                }

                // 读取单元格的值
                switch (cell.getCellType()) {
                    case STRING:
                        data[rowIndex][colIndex] = cell.getStringCellValue();
                        break;
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            // 如果是日期格式，存为 Date 对象
                            data[rowIndex][colIndex] = cell.getDateCellValue();
                        } else {
                            // 否则，是纯数字
                            double numericValue = cell.getNumericCellValue();

                            // 核心判断：检查 double 值是否等同于其整数转换值
                            // 例如：2000.0 == (int)2000.0 (即 2000.0 == 2000) -> true
                            // 例如：2000.5 == (int)2000.5 (即 2000.5 == 2000) -> false
                            if (numericValue == (int) numericValue) {
                                // 如果是整数，则将其转换为 int 类型存储
                                data[rowIndex][colIndex] = (int) numericValue;
                            } else {
                                // 如果是小数，则保留 double 类型
                                data[rowIndex][colIndex] = numericValue;
                            }
                        }
                        break;
                    case BOOLEAN:
                        data[rowIndex][colIndex] = cell.getBooleanCellValue();
                        break;
                    case FORMULA:
                        // 获取公式的计算结果
                        try {
                            CellValue cellValue = evaluateFormulaCellValue(cell);
                            if (cellValue.getCellType() == CellType.ERROR) {
                                // 处理错误
                                data[rowIndex][colIndex] = "公式计算出现错误";
                            } else {
                                if (cellValue.getCellType() == CellType.NUMERIC) {
                                    data[rowIndex][colIndex] = cellValue.getNumberValue();
                                } else if (cellValue.getCellType() == CellType.STRING) {
                                    data[rowIndex][colIndex] = cellValue.getStringValue();
                                } else if (cellValue.getCellType() == CellType.BOOLEAN) {
                                    data[rowIndex][colIndex] = cellValue.getBooleanValue();
                                }
                            }
                        } catch (NotImplementedException e) {
                            // 处理未实现的函数或其他异常
                            System.out.println("公式计算失败: " + e.getMessage());
                        }
                        break;
                    default:
                        data[rowIndex][colIndex] = null;
                        break;
                }
            }
        }
        return data;
    }

    public static Map<String, Object[][]> readExcel(Workbook workbook) {
        int numberOfSheets = workbook.getNumberOfSheets();
        Map<String, Object[][]> result = new TreeMap<>();
        for (int i = 0; i < numberOfSheets; i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet == null) {
                continue; // 跳过空的工作表
            }
            int maxRow = sheet.getLastRowNum() + 1;
            int maxCol = sheet.getRow(0).getLastCellNum();
            String sheetName = sheet.getSheetName();
            Object[][] data = readSheet(sheet, maxRow, maxCol);
            result.put(sheetName, data);
        }
        return result;
    }

    /**
     * 获取excel里面raw行，column列的数据
     *
     * @param fileName
     * @param sheetName
     * @param raw
     * @param column
     * @return
     */
    public static Object[][] readPartExcel(String fileName, String sheetName, int raw, int column){
        ZipSecureFile.setMinInflateRatio(-1.0d);
        try (InputStream is = getInputStreamSmart(fileName)) {
            Workbook workbook;
            if (is != null) {
                workbook = WorkbookFactory.create(is);
            } else {
                throw new RuntimeException("无法读取Excel文件，请检查文件路径和格式是否正确。");
            }
            if (workbook == null) {
                throw new RuntimeException("无法读取Excel文件或工作簿为空，请检查文件路径和格式是否正确。");
            }
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException("工作表 " + sheetName + " 不存在，请检查工作表名称是否正确。");
            }
            // 获取最大行数和列数
            int maxRow = Math.min(raw, sheet.getLastRowNum() + 1);
            if (sheet.getRow(0) == null) return null;
            int maxCol = Math.min(sheet.getRow(0).getLastCellNum(), column);
            Object[][] data = readSheet(sheet, maxRow, maxCol);
            workbook.close();
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeSheet(Workbook workbook, String sheetName, Object[][] data) {
        Sheet sheet = workbook.getSheet(sheetName); // 创建一个工作表
        // 先判断工作簿是否存在，不存在则创建，存在则清空
        if (sheet != null) {
            try {
                // 清空工作表中的数据
                for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        sheet.removeRow(row);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            sheet = workbook.createSheet(sheetName);
        }
        // 填充数据到工作表中
        for (int rowIndex = 0; rowIndex < data.length; rowIndex++) {
            Row row = sheet.createRow(rowIndex); // 创建行
            for (int colIndex = 0; colIndex < data[rowIndex].length; colIndex++) {
                Cell cell = row.createCell(colIndex); // 创建单元格
                // 将数据写入单元格
                if (data[rowIndex][colIndex] instanceof String) {
                    cell.setCellValue((String) data[rowIndex][colIndex]);
                } else if (data[rowIndex][colIndex] instanceof Integer) {
                    cell.setCellValue((Integer) data[rowIndex][colIndex]);
                } else if (data[rowIndex][colIndex] instanceof Double) {
                    cell.setCellValue((Double) data[rowIndex][colIndex]);
                } else if (data[rowIndex][colIndex] instanceof Boolean) {
                    cell.setCellValue((Boolean) data[rowIndex][colIndex]);
                } else if (data[rowIndex][colIndex] instanceof Date) {
                    // 写入日期时需要使用日期格式
                    cell.setCellValue((Date) data[rowIndex][colIndex]);
                    CellStyle cellStyle = workbook.createCellStyle();
                    CreationHelper createHelper = workbook.getCreationHelper();
                    cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd HH"));
                    cell.setCellStyle(cellStyle);
                } else {
                    // 处理其他类型或未知类型
                    cell.setCellValue(data[rowIndex][colIndex] != null ? data[rowIndex][colIndex].toString() : "");
                }
            }
        }
    }

    public static void writeExcel(String fileName, String sheetName, Object[][] data) {
        // 检查文件是否存在，如果不存在则创建文件
        File file = new File(fileName);
        ZipSecureFile.setMinInflateRatio(-1.0d);
        Workbook workbook = null;
        if (!file.exists()) {
            try {
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs(); // 创建父目录
                }
                file.createNewFile(); // 创建文件
                workbook = new XSSFWorkbook();// 创建一个工作簿
            } catch (IOException e) {
                e.printStackTrace();
                return; // 如果无法创建文件则退出
            }
        } else {
            try {
                FileInputStream fis = new FileInputStream(fileName);
                workbook = new XSSFWorkbook(fis);
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Sheet sheet = workbook.getSheet(sheetName); // 创建一个工作表
        // 先判断工作簿是否存在，不存在则创建，存在则清空
        if (sheet != null) {
            try {
                // 清空工作表中的数据
                for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        sheet.removeRow(row);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            sheet = workbook.createSheet(sheetName);
        }

        // 填充数据到工作表中
        for (int rowIndex = 0; rowIndex < data.length; rowIndex++) {
            Row row = sheet.createRow(rowIndex); // 创建行
            for (int colIndex = 0; colIndex < data[rowIndex].length; colIndex++) {
                Cell cell = row.createCell(colIndex); // 创建单元格
                // 将数据写入单元格
                if (data[rowIndex][colIndex] instanceof String) {
                    cell.setCellValue((String) data[rowIndex][colIndex]);
                } else if (data[rowIndex][colIndex] instanceof Integer) {
                    cell.setCellValue((Integer) data[rowIndex][colIndex]);
                } else if (data[rowIndex][colIndex] instanceof Double) {
                    cell.setCellValue((Double) data[rowIndex][colIndex]);
                } else if (data[rowIndex][colIndex] instanceof Boolean) {
                    cell.setCellValue((Boolean) data[rowIndex][colIndex]);
                } else if (data[rowIndex][colIndex] instanceof Date) {
                    // 写入日期时需要使用日期格式
                    cell.setCellValue((Date) data[rowIndex][colIndex]);
                    CellStyle cellStyle = workbook.createCellStyle();
                    CreationHelper createHelper = workbook.getCreationHelper();
                    cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd HH"));
                    cell.setCellStyle(cellStyle);
                } else {
                    // 处理其他类型或未知类型
                    cell.setCellValue(data[rowIndex][colIndex] != null ? data[rowIndex][colIndex].toString() : "");
                }
            }
        }

        // 写入 Excel 文件
        try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
            workbook.write(fileOut); // 将工作簿写入文件
            System.out.println("数据成功写入文件: " + fileName + "  " + sheetName);
        } catch (IOException e) {
            System.err.println("无法写入文件，因为它可能正被另一个程序占用。请关闭文件然后重试。");
            e.printStackTrace();
        } finally {
            try {
                workbook.close(); // 关闭工作簿
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查是否为合并单元格
     *
     * @param sheet
     * @param rowIndex
     * @param colIndex
     * @return
     */
    private static boolean isMergedCell(Sheet sheet, int rowIndex, int colIndex) {
        int numMergedRegions = sheet.getNumMergedRegions();
        for (int i = 0; i < numMergedRegions; i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            if (range.isInRange(rowIndex, colIndex)) {
                // 只有第一个单元格包含数据
                return range.getFirstRow() != rowIndex || range.getFirstColumn() != colIndex;
            }
        }
        return false; // 不是合并单元格
    }

    /**
     * 计算单元格内的公式
     *
     * @param cell
     * @return
     */
    private static CellValue evaluateFormulaCellValue(Cell cell) {
        Workbook workbook = cell.getSheet().getWorkbook();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        CellValue cellValue = null;
        try {
            cellValue = evaluator.evaluate(cell);
            return cellValue;
        } catch (RuntimeException e) {
            return new CellValue("error");
        }

    }

    /**
     * 输出该文件中所有的sheet表
     */
    public static List<String> checkSheetsInExcel(String path) {
        ZipSecureFile.setMinInflateRatio(-1.0d);
        List<String> sheets = new ArrayList<>();

        try (InputStream is = getInputStreamSmart(path)) {
            if (is == null) {
                System.err.println("无法读取文件：" + path);
                return sheets;
            }

            Workbook workbook = WorkbookFactory.create(is);
            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {
                sheets.add(workbook.getSheetAt(i).getSheetName());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sheets;
    }

    private static InputStream getInputStreamSmart(String path) {
        try {
            File file = new File(path);

            //  如果是绝对路径 or 存在于当前目录下（自动转绝对路径）
            if (file.isAbsolute() || file.exists()) {
                System.out.println("读取文件系统路径: " + file.getAbsolutePath());
                return new FileInputStream(file.getAbsoluteFile());
            }

            //  否则尝试从 classpath 加载（如 resources 目录）
            InputStream resourceStream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(path);

            if (resourceStream != null) {
                System.out.println("读取 classpath 路径: " + path);
                return resourceStream;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

}
