package com.hzmlearning.appoinment.doctor.biz.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hzmlearning.appoinment.doctor.biz.model.dto.Doctor;
import com.hzmlearning.appoinment.doctor.biz.model.dto.HalfDaySchedule;
import com.hzmlearning.appoinment.doctor.biz.model.dto.ScheduleDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author huangzm
 * @Date 2023/3/28 9:52
 */
@Log4j2
@Service
public class AppointService {
    @Value("${weixin.cookie}")
    private String cookieStr;

    @Value("${weixin.dep.id}")
    private String depId;

    @Value("${weixin.unit.id}")
    private String unitId;

    @Value("${weixin.search.time}")
    private String searchTime;

    @Value("${weixin.expect.doctor}")
    private String expectDoctors;

    @Value("${weixin.expect.date}")
    private String expectDate;

    private static final String METHOD = "sch1";
    private static final String BRANCH_ID = "200051254";



    public void appoint(){
        List<Doctor> doctors = getDoctors();
        List<Doctor> expectDoctors = matchesExpectDoctors(doctors);
        boolean fail = false;
        for (Doctor doctor : doctors) {
            fail = appointDoctor(doctor);
            if(!fail){
                break;
            }
        }
        if(fail){
            //成功

        }



    }


    private List<Doctor> getDoctors(){
        //获取医生排班
        Map<String,Object> map = new HashMap<>();
        map.put("depId", depId);
        map.put("unitId", unitId);
        map.put("searchTime", searchTime);
        HttpResponse response = HttpRequest.post("https://wxis.91160.com/wxis/doc/getDocListByTime.do")
                .cookie(cookieStr)
                .form(map)
                .execute();

        String body = response.body();
        JSONObject apiResponse = JSONUtil.toBean(body, JSONObject.class);
        if(!apiResponse.getStr("code").equals("success")){
            log.info(" 请求获取医生排班接口失败: {}", body);
            return Lists.newArrayList();
        }
        JSONObject data = (JSONObject) apiResponse.get("data");
        JSONArray rows = data.getJSONArray("rows");
        return JSONUtil.toList(rows, Doctor.class);
    }


    /**
     * 匹配医生
     * @param doctors
     * @return
     */
    private List<Doctor> matchesExpectDoctors(List<Doctor> doctors){
        List<String> expectDoctorList = StrUtil.splitTrim(expectDoctors, ",");
        List<Doctor> targetDoctors = new ArrayList<>();
        for (String name : expectDoctorList) {
            for (Doctor doctor : doctors) {
                //yuyueState 2表示可预约
                if (doctor.getDoctorName().indexOf(name) > -1 && doctor.getYuyueState().equals("2")) {
                    targetDoctors.add(doctor);
                }
            }
        }
        return targetDoctors;
    }


    /**
     * 预约医生
     * @param doctor
     * @return
     */
    private boolean appointDoctor(Doctor doctor){
        boolean isFail = false;
        List<ScheduleDTO> doctorSchedules = getDoctorSchedule(doctor);
        //上午 下午
        List<ScheduleDTO> expectSchedules = matchesExpectSchedule(doctorSchedules);
        for (ScheduleDTO expectSchedule : expectSchedules) {
            List<HalfDaySchedule>  halfDaySchedules = getHalfDaySchedule(expectSchedule);
            for (HalfDaySchedule halfDaySchedule : halfDaySchedules) {

            }
        }
        return isFail;
    }

    /**
     * 医生上午或下午的安排
     * @param expectSchedule
     * @return
     */
    private List<HalfDaySchedule> getHalfDaySchedule(ScheduleDTO expectSchedule) {
        JSONObject obj = new JSONObject();
        obj.set("unit_id", unitId);
        obj.set("doctor_id", expectSchedule.getDoctor_id());
        obj.set("dep_id", depId);
        obj.set("schedule_id", expectSchedule.getSchedule_id());
        obj.set("time_type", expectSchedule.getTime_type());

        JSONArray array = new JSONArray();
        array.add(obj);

        String url = MessageFormat.format("https://wxis.91160.com/wxis/sch_new/detlnew.do?unit_detl_map={0}",
                JSONUtil.toJsonStr(array));
        String body = HttpRequest.post(url)
                .cookie(cookieStr)
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .execute()
                .body();
        log.info("getHalfDaySchedule body : {}", body);
        JSONObject apiResponse = JSONUtil.toBean(body, JSONObject.class);
        if(!apiResponse.getStr("status").equals("1")){
            log.info("getHalfDaySchedule 接口失败: {}", body);
            return Lists.newArrayList();
        }
        JSONArray data =apiResponse.getJSONArray("data");
        return JSONUtil.toList(data, HalfDaySchedule.class);
    }

    private List<ScheduleDTO> getDoctorSchedule(Doctor doctor){
        //unit_id=138&dep_id=4703&doctor_id=5565&cur_dep_id=4703&unit_name=中国科学院大学深圳医院&dep_name=口腔科(西院区)
        Map<String,Object> map = Maps.newHashMap();
        map.put("depId", depId);
        map.put("unitId", unitId);
        map.put("doctor_id", doctor.getDoctorId());
        map.put("cur_dep_id", depId);
        map.put("unit_name", "");
        map.put("dep_name", "");
        String url = MessageFormat.format("https://wxis.91160.com/wxis/sch_new/schedulelist.do?unit_id={0}&dep_id={1}&doctor_id={2}&cur_dep_id={3}&unit_name={4}&dep_name={5}",
                unitId, depId, doctor.getDoctorId(), depId, "中国科学院大学深圳医院", "口腔科(西院区)");
        String body = HttpRequest.post(url)
                .cookie(cookieStr)
                .header("Accept", "application/json")
                .execute()
                .body();

        log.info("getDoctorSchedule body : {}", body);
        JSONObject apiResponse = JSONUtil.toBean(body, JSONObject.class);
        if(!apiResponse.getStr("status").equals("1")){
            log.info("请求获取医生排班接口失败: {}", body);
            return Lists.newArrayList();
        }
        JSONObject data = (JSONObject) apiResponse.get("data");
        JSONArray rows = data.getJSONArray("sch");
        return JSONUtil.toList(rows, ScheduleDTO.class);
    }


    /**
     * 按期望日期匹配该医生的排班
     * @param scheduleDTOS
     * @return
     */
    private List<ScheduleDTO> matchesExpectSchedule(List<ScheduleDTO> scheduleDTOS){
        return scheduleDTOS.stream().filter(e -> e.getTo_date().equals(expectDate) && e.getY_state().equals("1")).collect(Collectors.toList());
    }

}
