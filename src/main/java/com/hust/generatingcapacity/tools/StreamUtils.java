package com.hust.generatingcapacity.tools;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class StreamUtils {

    /**
     * 根据对象的某个 key 去重
     *
     * @param keyExtractor 提取 key 的函数 (例如 obj -> obj.getUnitName())
     * @param <T>          元素类型
     * @return 可用于 Stream.filter() 的 Predicate
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
