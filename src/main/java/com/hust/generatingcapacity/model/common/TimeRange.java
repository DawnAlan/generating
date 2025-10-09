package com.hust.generatingcapacity.model.common;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Data
@Getter
@Setter
public class TimeRange {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date start;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date end;

    public TimeRange(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    public TimeRange() {}

    public static Boolean isInRange(Date date, TimeRange timeRange) {
        if (date == null || timeRange == null) {
            return false;
        }
        return !date.before(timeRange.getStart()) && !date.after(timeRange.getEnd());
    }

    public Boolean isEmpty() {
        return this.start == null || this.end == null;
    }
}
