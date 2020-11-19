package com.kevin.threads.base.localDateTime;

import com.kevin.common.utils.time.LocalDateTimeUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author kevin
 * @date 2020-11-16 10:00:47
 * @desc
 */
public class FaceLocalDate {

    public static void main(String[] args) {
        faceAtStartOfDay();
    }

    public static void faceAtStartOfDay() {
        LocalDate now = LocalDate.now();
        LocalDateTime localDateTimeStart = now.atStartOfDay();
        System.out.println(LocalDateTimeUtils.parseTime(localDateTimeStart));
    }


}
