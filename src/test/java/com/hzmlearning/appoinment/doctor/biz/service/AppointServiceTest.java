package com.hzmlearning.appoinment.doctor.biz.service;

import com.hzmlearning.appoinment.doctor.DoctorApplicationTests;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;

/**
 * @Author huangzm
 * @Date 2023/3/28 9:57
 */
class AppointServiceTest extends DoctorApplicationTests {
    @Resource
    private AppointService appointService;

    @Test
    public void test(){
        appointService.appoint();
    }

}
