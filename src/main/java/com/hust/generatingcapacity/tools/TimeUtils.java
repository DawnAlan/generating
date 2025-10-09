package com.hust.generatingcapacity.tools;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {

    static SimpleDateFormat sdfd = new SimpleDateFormat("yyyy-MM-dd");
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    /**
     * @param date Date类型数据
     * @return xx年xx月xx日xx时xx分xx秒
     */
    public static String dateToStr(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR) + "年" + (calendar.get(Calendar.MONTH) + 1) + "月" + calendar.get(Calendar.DAY_OF_MONTH) + "日"
                + calendar.get(Calendar.HOUR_OF_DAY) + "时" + calendar.get(Calendar.MINUTE) + "分" + calendar.get(Calendar.SECOND) + "秒";
    }

    public static Date convertToDate(LocalDate localDate) {
        // 将 LocalDate 转换为 ZonedDateTime
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.systemDefault());
        // 将 ZonedDateTime 转换为 Instant
        return Date.from(zonedDateTime.toInstant());
    }

    public static Date cleanDate(Date date,String period) {
        Date result = null;
        switch (period) {
            case "日":
                LocalDate localDate = convertToLocalDate(date);
                result = convertToDate(localDate);
                break;
            case "小时":
                LocalDateTime localDateTime = convertToLocalDateTime(date);
                // 将 LocalDateTime 截断到小时
                localDateTime = localDateTime.truncatedTo(ChronoUnit.HOURS);
                result = convertToDate(localDateTime);
                break;
            case "分钟":
                LocalDateTime localDateTimeMin = convertToLocalDateTime(date);
                // 将 LocalDateTime 截断到分钟
                localDateTimeMin = localDateTimeMin.truncatedTo(ChronoUnit.MINUTES);
                result = convertToDate(localDateTimeMin);
                break;
            case "秒":
                LocalDateTime localDateTimeSec = convertToLocalDateTime(date);
                // 将 LocalDateTime 截断到秒
                localDateTimeSec = localDateTimeSec.truncatedTo(ChronoUnit.SECONDS);
                result = convertToDate(localDateTimeSec);
                break;
        }
        return result;

    }

    public static LocalDateTime convertToLocalDateTime(Date date) {
        // 将 Date 转换为 Instant
        Instant instant = date.toInstant();
        // 将 Instant 转换为 LocalDateTime
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static Date convertToDate(LocalDateTime localDateTime) {
        // 将 LocalDateTime 转换为 ZonedDateTime
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        // 将 ZonedDateTime 转换为 Instant
        return Date.from(zonedDateTime.toInstant());
    }

    public static LocalDate convertToLocalDate(Date date) {
        // 将 Date 转换为 Instant
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    /**
     * 从String里面提取日期
     *
     * @param string
     * @return
     */
    public static List<Date> strToDate(String string) {
        // 定义正则表达式来匹配日期格式
        String regex = "\\d{4}-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])(?!\\d)";
        // 检查是否符合 "yyyy-MM-dd" 格式
        Pattern yyyy_MM_dd = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])");
        // 检查是否符合 "yyyy-M-dd" 格式
        Pattern yyyy_M_dd = Pattern.compile("\\d{4}-([1-9])-(0[1-9]|[12][0-9]|3[01])");
        // 检查是否符合 "yyyy-MM-d" 格式
        Pattern yyyy_MM_d = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])-([1-9])(?!\\d)");
        // 检查是否符合 "yyyy-M-d" 格式
        Pattern yyyy_M_d = Pattern.compile("\\d{4}-([1-9])-([1-9])(?!\\d)");


        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);

        // 定义日期格式
        DateTimeFormatter formatter_MM_dd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatter_M_dd = DateTimeFormatter.ofPattern("yyyy-M-dd");
        DateTimeFormatter formatter_MM_d = DateTimeFormatter.ofPattern("yyyy-MM-d");
        DateTimeFormatter formatter_M_d = DateTimeFormatter.ofPattern("yyyy-M-d");

        // 输入字符串
        Matcher matcher = pattern.matcher(string);

        // 用于存储解析后的日期
        List<Date> dates = new ArrayList<>();

        // 查找并解析日期
        while (matcher.find()) {
            try {
                String dateStr = matcher.group(); // 取出匹配的日期字符串
                Matcher matcherYYYYMMDD = yyyy_MM_dd.matcher(dateStr);
                Matcher matcherYYYYMDD = yyyy_M_dd.matcher(dateStr);
                Matcher matcherYYYYMMD = yyyy_MM_d.matcher(dateStr);
                Matcher matcherYYYYMD = yyyy_M_d.matcher(dateStr);
                LocalDate localDate = null;
                if (matcherYYYYMMDD.matches()) {
                    localDate = LocalDate.parse(dateStr, formatter_MM_dd);
                } else if (matcherYYYYMDD.matches()) {
                    localDate = LocalDate.parse(dateStr, formatter_M_dd);
                } else if (matcherYYYYMMD.matches()) {
                    localDate = LocalDate.parse(dateStr, formatter_MM_d);
                } else if (matcherYYYYMD.matches()) {
                    localDate = LocalDate.parse(dateStr, formatter_M_d);
                }
                assert localDate != null;
                Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                dates.add(date);
            } catch (DateTimeParseException e) {
                System.err.println("Failed to parse date: " + matcher.group(1));
            }
        }
        return dates;
    }

    /**
     * 获得数据中的年、月、日、小时、分钟
     */
    public static Map<String, Integer> getSpecificDate(Date date) {
        Map<String, Integer> result = new HashMap<>();
        int year;
        int month;
        int day;
        int hour;
        int minute;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH) + 1;
        day = cal.get(Calendar.DAY_OF_MONTH);
        hour = cal.get(Calendar.HOUR_OF_DAY);
        minute = cal.get(Calendar.MINUTE);
        result.put("年", year);
        result.put("月", month);
        result.put("日", day);
        result.put("小时", hour);
        result.put("分钟", minute);
        return result;
    }

    public static Integer getLastDayOfMonth(int year, int month) {
        YearMonth yearMonthFeb = YearMonth.of(year, month);
        return yearMonthFeb.lengthOfMonth();
    }

    //将秒数转换为具体的时间
    public static String formatSecondsToHMS(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("小时 ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("分钟 ");
        }
        if (secs > 0 || sb.isEmpty()) { // 如果前面都为0，也至少显示“0秒”
            sb.append(secs).append("秒");
        }
        return sb.toString().trim();
    }

    /**
     * 日期比较
     *
     * @param date1 最终返回date2=date1
     * @param date2 最终返回date2=date1
     * @return 如果两个日期在规定尺度上相等，则返回true
     */
    public static Boolean dateCompare(Date date1, Date date2, String period) {
        boolean result = false;
        int year = getSpecificDate(date1).get("年");
        int month = getSpecificDate(date1).get("月");
        int day = getSpecificDate(date1).get("日");
        int hour = getSpecificDate(date1).get("小时");
        int year1 = getSpecificDate(date2).get("年");
        int month1 = getSpecificDate(date2).get("月");
        int day1 = getSpecificDate(date2).get("日");
        int hour1 = getSpecificDate(date2).get("小时");
        if (period.equals("小时")) {
            if (year1 == year & month1 == month & day1 == day & hour1 == hour) {
                result = true;
            }
        }
        if (period.equals("日")) {
            if (year1 == year & month1 == month & day1 == day) {
                result = true;
            }
        }
        if (period.equals("月")) {
            if (year1 == year & month1 == month) {
                result = true;
            }
        }
        if (period.equals("年")) {
            if (year1 == year) {
                result = true;
            }
        }
        return result;
    }

    /**
     * date1早于或者等于date2
     * @param date1
     * @param date2
     * @param period
     * @return
     */
    public static Boolean isBeforeOrSame(Date date1, Date date2, String period){
        return dateCompare(date1,date2,period)||date1.before(date2);
    }

    public static Boolean isAfterOrSame(Date date1, Date date2, String period){
        return dateCompare(date1,date2,period)||date1.after(date2);
    }

    /**
     * 添加时间
     */
    public static Date addCalendar(Date startDate, String period, int l) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        switch (period) {
            case "年":
                cal.add(Calendar.YEAR, l);
                break;
            case "月":
                cal.add(Calendar.MONTH, l);
                break;
            case "日":
                cal.add(Calendar.DAY_OF_MONTH, l);
                break;
            case "小时":
                cal.add(Calendar.HOUR_OF_DAY, l);
                break;
            case "分钟":
                cal.add(Calendar.MINUTE, l);
                break;
        }
        return cal.getTime();
    }

    /**
     * 返回的是这个时间或者他后面离他最近的值
     *
     * @param timeSeries 按升序排列的时间list
     * @param inputTime  需要寻找的时间
     */
    public static int findNearestTime(List<Date> timeSeries, Date inputTime) {
        Collections.sort(timeSeries);
        int index = Collections.binarySearch(timeSeries, inputTime);
        if (index >= 0) {
            return index; // 输入时间点恰好存在于时间序列中
        }
        index = -index - 1; // 找到输入时间点应该插入的位置
        if (index == 0) {
            return 0; // 输入时间点比时间序列中最小的时间还小
        }
        if (index == timeSeries.size()) {
            return timeSeries.size() - 1; // 输入时间点比时间序列中最大的时间还大
        }
        return index;
    }

    /**
     * 获取两个日期间的连续Date列表
     *
     * @param start
     * @param end
     * @param period
     * @return
     */
    public static List<Date> getDateList(Date start, Date end, String period) {
        List<Date> result = new ArrayList<>();
        int l = getDateDuration(start, end, period);
        for (int i = 0; i <= l; i++) {
            result.add(addCalendar(start, period, i));
        }
        return result;
    }


    /**
     * 返回日期相差的数量（小时，日，月）
     * 后面减去前面
     */

    public static int getDateDuration(Date dateStart, Date dateEnd, String period) {
        if (dateStart == null || dateEnd == null || period == null) return 0;

        LocalDateTime start = dateStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime end = dateEnd.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        switch (period) {
            case "年":
                return end.getYear() - start.getYear();
            case "月":
                YearMonth startYearMonth = YearMonth.from(start);
                YearMonth endYearMonth = YearMonth.from(end);
                return (int) ChronoUnit.MONTHS.between(startYearMonth, endYearMonth);
            case "旬": {
                int months = Period.between(start.toLocalDate(), end.toLocalDate()).getYears() * 12
                        + Period.between(start.toLocalDate(), end.toLocalDate()).getMonths();
                int dayStart = start.getDayOfMonth();
                int dayEnd = end.getDayOfMonth();
                int xunStart = dayStart <= 10 ? 1 : (dayStart <= 20 ? 2 : 3);
                int xunEnd = dayEnd <= 10 ? 1 : (dayEnd <= 20 ? 2 : 3);
                return months * 3 + xunEnd - xunStart;
            }
            case "日":
                return (int) ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
            case "小时":
                LocalDateTime startTruncatedToHour = start.truncatedTo(ChronoUnit.HOURS);
                LocalDateTime endTruncatedToHour = end.truncatedTo(ChronoUnit.HOURS);
                return (int) ChronoUnit.HOURS.between(startTruncatedToHour, endTruncatedToHour);
            case "分钟":
                LocalDateTime startTruncatedToMinute = start.truncatedTo(ChronoUnit.MINUTES);
                LocalDateTime endTruncatedToMinute = end.truncatedTo(ChronoUnit.MINUTES);
                return (int) ChronoUnit.MINUTES.between(startTruncatedToMinute, endTruncatedToMinute);
            case "秒":
                LocalDateTime startTruncatedToSecond = start.truncatedTo(ChronoUnit.SECONDS);
                LocalDateTime endTruncatedToSecond = end.truncatedTo(ChronoUnit.SECONDS);
                return (int) ChronoUnit.SECONDS.between(startTruncatedToSecond, endTruncatedToSecond);
            default:
                throw new IllegalArgumentException("不支持的 period 单位: " + period);
        }
    }

    /**
     * 找出不连续的时间范围
     *
     * @param dates 日期列表
     * @return 时间范围列表
     */
    public static List<String> findDateRanges(List<Date> dates) {
        List<String> ranges = new ArrayList<>();
        if (dates == null || dates.isEmpty()) {
            return ranges;
        }

        // 按时间排序
        Collections.sort(dates);

        // 初始化起始日期
        Date startDate = dates.get(0);
        Date previousDate = startDate;

        for (int i = 1; i < dates.size(); i++) {
            Date currentDate = dates.get(i);
            // 计算前一个日期与当前日期的天数差
            long dayDifference = (currentDate.getTime() - previousDate.getTime()) / (24 * 60 * 60 * 1000);

            // 如果差值大于1天，表示不连续
            if (dayDifference > 1) {
                // 添加当前范围到列表
                ranges.add(formatDateRange(startDate, previousDate));
                // 更新起始日期
                startDate = currentDate;
            }
            previousDate = currentDate;
        }

        // 添加最后一个范围
        ranges.add(formatDateRange(startDate, previousDate));

        return ranges;
    }

    /**
     * 格式化日期范围
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 格式化的日期范围字符串
     */
    public static String formatDateRange(Date start, Date end) {
        return formatDate(start) + " - " + formatDate(end);
    }

    /**
     * 格式化日期为字符串
     *
     * @param date 日期
     * @return 格式化的日期字符串
     */
    public static String formatDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return "" + cal.get(Calendar.YEAR) + "-"
                + (cal.get(Calendar.MONTH) + 1) + "-"
                + cal.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 创建指定日期的Date对象
     *
     * @param year  年
     * @param month 月
     * @param day   日
     * @return 日期对象
     */
    public static Date createDate(int year, int month, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, hour, minute);
        return cal.getTime();
    }

    public static Date createDate(int year, int month, int day, int hour, int minute, int sec) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, hour, minute, sec);
        return cal.getTime();
    }

}
