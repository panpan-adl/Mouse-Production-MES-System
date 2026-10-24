package com.mouse.mes.controller;

import com.mouse.mes.entity.Attendance;
import com.mouse.mes.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendances")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping
    public List<Attendance> getAllAttendances() {
        return attendanceService.findAll();
    }

    @GetMapping("/{id}")
    public Attendance getAttendanceById(@PathVariable Long id) {
        return attendanceService.findById(id);
    }

    @GetMapping("/user/{userId}")
    public List<Attendance> getAttendancesByUserId(@PathVariable Long userId) {
        return attendanceService.findByUserId(userId);
    }

    @GetMapping("/date/{attendanceDate}")
    public List<Attendance> getAttendancesByAttendanceDate(@PathVariable String attendanceDate) {
        return attendanceService.findByAttendanceDate(attendanceDate);
    }

    @PostMapping
    public Attendance createAttendance(@RequestBody Attendance attendance) {
        return attendanceService.create(attendance);
    }

    @PutMapping("/{id}")
    public Attendance updateAttendance(@PathVariable Long id, @RequestBody Attendance attendance) {
        attendance.setId(id);
        return attendanceService.update(attendance);
    }
}