package com.hust.generatingcapacity.tools;

import lombok.SneakyThrows;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileUtils {


    /**
     * 获取某个地址下所有文件名
     * @param directoryPath
     * @return
     */
    public static List<String> getFileNames(String directoryPath){
        List<String> names = new ArrayList<>();
        File directory = new File(directoryPath);
        if (directory.isDirectory()) {
            // 获取文件夹内的所有文件
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.getName().startsWith("~$")) {
                        names.add(file.getName());
                    }
                }
                Collections.sort(names);
            } else {
                throw new RuntimeException(directoryPath + " 文件夹为空或无法访问。");
            }
        } else {
            throw new RuntimeException(directoryPath + "  指定的路径不是一个文件夹。");
        }
        return names;
    }



    /**
     * 将某个文件重命名
     * @param directoryPath
     * @param targetFileName
     * @param newFileName
     */
    public void renameFile(String directoryPath, String targetFileName, String newFileName) {
        File directory = new File(directoryPath);

        // 检查目录是否存在
        if (!directory.exists() || !directory.isDirectory()) {
            throw new RuntimeException("指定的路径不是一个有效的目录。");
        }

        // 查找目标文件
        File targetFile = new File(directory, targetFileName);

        // 检查目标文件是否存在
        if (!targetFile.exists() || !targetFile.isFile()) {
            throw new RuntimeException("指定的文件不存在: " + targetFileName);
        }

        // 创建新的文件对象
        File newFile = new File(directory, newFileName);

        // 重命名文件
        if (!targetFile.renameTo(newFile)) {
            throw new RuntimeException("文件重命名失败: " + targetFileName);
        }
    }


    /**
     * 导出txt文件
     * @param filePath
     * Txt文件(路径)
     * @param fileName
     * Txt文件(文件名)，Txt文件不存在会自动创建
     * @param dataList
     * 数据
     * @return
     */
    @SneakyThrows
    public static void exportTxt(String filePath, String fileName, List<String> dataList) {
        File pathFile = new File(filePath);
        // 检查目录是否存在，如不存在则创建
        if (!pathFile.exists()) {
            pathFile.mkdirs(); // 使用 mkdirs() 以确保可以创建多层目录
        }

        // File.separator 文件分隔符
        String relFilePath = filePath + File.separator + fileName;
        File file = new File(relFilePath);

        FileOutputStream out = null;
        try {
            // 设置 append 为 true 以便在文件存在时继续追加内容
            out = new FileOutputStream(file, true);

            // 调用方法写入 dataList 的内容
            exportTxtByOS(out, dataList);

        } catch (FileNotFoundException e) {
            System.out.println("文件未找到: " + e.getMessage());
        }  finally {
            // 确保文件流被关闭
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 导出
     *
     * @param out
     *            输出流
     * @param dataList
     *            数据
     * @return
     */
    public static boolean exportTxtByOS(OutputStream out, List<String> dataList) {
        boolean isSucess = false;
        OutputStreamWriter osw = null;
        BufferedWriter bw = null;
        try {
            osw = new OutputStreamWriter(out);
            bw = new BufferedWriter(osw);
            // 循环数据
            for (int i = 0; i < dataList.size(); i++) {
                bw.append(dataList.get(i)).append("\r\n");
            }

            isSucess = true;
        } catch (Exception e) {
            e.printStackTrace();
            isSucess = false;

        } finally {
            if (bw != null) {
                try {
                    bw.close();
                    bw = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (osw != null) {
                try {
                    osw.close();
                    osw = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                    out = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return isSucess;
    }
}
