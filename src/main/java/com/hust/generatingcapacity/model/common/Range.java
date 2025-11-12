package com.hust.generatingcapacity.model.common;

public class Range<T extends Comparable<? super T>> {

    private final T start;
    private final T end;

    public Range(T start, T end) {
        this.start = start;
        this.end = end;
    }

    public T getStart() { return start; }
    public T getEnd() { return end; }

    public boolean isEmpty() {
        return start == null || end == null;
    }

    /** 闭区间 [start, end] 判断 */
    public boolean contains(T value) {
        if (value == null || isEmpty()) {
            return false;
        }
        return value.compareTo(start) >= 0 && value.compareTo(end) <= 0;
    }

    /** 静态工具：判某个值是否在某个区间内 */
    public static <T extends Comparable<? super T>> boolean isInRange(T value, Range<T> range) {
        return range != null && range.contains(value);
    }
}

