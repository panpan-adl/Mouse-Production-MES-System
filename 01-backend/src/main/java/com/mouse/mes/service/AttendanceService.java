package com.mouse.mes.service;

import com.mouse.mes.entity.Attendance;

import java.util.List;

public interface AttendanceService {
    List<Attendance> findAll();
    Attendance findById(Long id);
    List<Attendance> findByUserId(Long userId);
    List<Attendance> findByAttendanceDate(String attendanceDate);
    Attendance create(Attendance attendance);
    Attendance update(Attendance attendance);
}