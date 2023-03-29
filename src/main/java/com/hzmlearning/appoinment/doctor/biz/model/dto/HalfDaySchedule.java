package com.hzmlearning.appoinment.doctor.biz.model.dto;

import lombok.Data;

/**
 * @Author huangzm
 * @Date 2023/3/28 14:00
 */
@Data
public class HalfDaySchedule {
    private String begin_time;
    private String detl_id;
    private String detl_time;
    private String schedule_id;
    private String srcext_type;
    private String yuyue_max;
    private String yuyue_num;
}
