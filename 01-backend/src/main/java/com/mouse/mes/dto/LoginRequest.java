package com.mouse.mes.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String userCode;
    private String username;
    private String password;
}