package com.hust.generatingcapacity.tools;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CustomBeanUtils {

    /**
     * 获取一个对象中所有值为null的属性名
     * @param source 源对象
     * @return 值为null的属性名数组
     */
    public static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    /**
     * 复制源对象中非空的属性到目标对象
     * @param source 源对象
     * @param destination 目标对象
     */
    public static void copyNonNullProperties(Object source, Object destination) {
        BeanUtils.copyProperties(source, destination, getNullPropertyNames(source));
    }

    public static void copyNonNullProperties(Object source, Object destination, String... ignoreProperties) {
        // 将忽略的属性名与null属性名合并
        Set<String> ignoreSet = new HashSet<>(Set.of(ignoreProperties));
        ignoreSet.addAll(Arrays.asList(getNullPropertyNames(source)));
        // 将Set转换为数组
        String[] ignoreArray = ignoreSet.toArray(new String[0]);
        // 复制属性，忽略null属性和指定的属性
        BeanUtils.copyProperties(source, destination, ignoreArray);
    }
}
