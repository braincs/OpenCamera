package com.braincs.attrsc.opencamera.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Shuai
 * 2020-07-05.
 */
public class DateUtil {
    public static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd";
    public static final String SIMPLE_DATE_TIME_FORMAT = "yyyy-MM-dd_HH_mm_ss";
    public static final String SIMPLE_DATE_TIME_MS_FORMAT = "yyyy-MM-dd_HH_mm_ss_SSS";
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.CHINA);
    private static final SimpleDateFormat simpleDateTimeFormat = new SimpleDateFormat(SIMPLE_DATE_TIME_FORMAT, Locale.CHINA);
    private static final SimpleDateFormat simpleDateTimeMsFormat = new SimpleDateFormat(SIMPLE_DATE_TIME_MS_FORMAT, Locale.CHINA);

    public static String getDate() {
        Date now = new Date();
        return simpleDateFormat.format(now);
    }

    public static String getDateTime() {
        Date now = new Date();
        return simpleDateTimeFormat.format(now);
    }

    public static String getDateTimeMs() {
        Date now = new Date();
        return simpleDateTimeMsFormat.format(now);
    }

    public static String getTimeStamp(){
        return ((Long)System.currentTimeMillis()).toString();
//        return ((Long)(System.currentTimeMillis()1000)).toString();
    }
}
