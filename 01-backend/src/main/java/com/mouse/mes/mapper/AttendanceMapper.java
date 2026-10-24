package com.mouse.mes.mapper;

import com.mouse.mes.entity.Attendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttendanceMapper {
    List<Attendance> findAll();
    Attendance findById(@Param("id") Long id);
    List<Attendance> findByUserId(@Param("userId") Long userId);
    List<Attendance> findByAttendanceDate(@Param("attendanceDate") String attendanceDate);
    int insert(Attendance attendance);
    int update(Attendance attendance);
}