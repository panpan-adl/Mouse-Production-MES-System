package com.mouse.mes.impl;

import com.mouse.mes.entity.Attendance;
import com.mouse.mes.mapper.AttendanceMapper;
import com.mouse.mes.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Override
    public List<Attendance> findAll() {
        return attendanceMapper.findAll();
    }

    @Override
    public Attendance findById(Long id) {
        return attendanceMapper.findById(id);
    }

    @Override
    public List<Attendance> findByUserId(Long userId) {
        return attendanceMapper.findByUserId(userId);
    }

    @Override
    public List<Attendance> findByAttendanceDate(String attendanceDate) {
        return attendanceMapper.findByAttendanceDate(attendanceDate);
    }

    @Override
    public Attendance create(Attendance attendance) {
        attendance.setCreateTime(new Date());
        attendanceMapper.insert(attendance);
        return attendance;
    }

    @Override
    public Attendance update(Attendance attendance) {
        attendanceMapper.update(attendance);
        return attendanceMapper.findById(attendance.getId());
    }
}