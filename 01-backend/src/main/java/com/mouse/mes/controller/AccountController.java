package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.service.AccountService;
import com.mouse.mes.service.AvatarStorageService;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService accounts;
    private final AvatarStorageService avatars;
    private final ObjectMapper json;
    public AccountController(AccountService accounts, AvatarStorageService avatars, ObjectMapper json) {this.accounts=accounts;this.avatars=avatars;this.json=json;}
    private long id(HttpServletRequest request) { return json.convertValue(request.getAttribute("userInfo"),LoginResponse.class).getId(); }
    public record ProfileInput(String realName,String phone,String email,Integer gender) {}
    public record PasswordInput(String oldPassword,String newPassword,String confirmPassword) {}
    public record ProtectionInput(Boolean enabled) {}
    @GetMapping("/profile") public Object profile(HttpServletRequest request) {return JsonResult.success(accounts.profile(id(request)));}
    @PutMapping("/profile") public Object update(@RequestBody ProfileInput input,HttpServletRequest request) {
        return JsonResult.success(accounts.updateProfile(id(request),input.realName(),input.phone(),input.email(),input.gender()));
    }
    @PostMapping("/avatar") public Object avatar(@RequestParam("file") MultipartFile file,HttpServletRequest request) throws IOException {
        avatars.upload(id(request),file); return JsonResult.success(accounts.profile(id(request)));
    }
    @PostMapping("/password") public Object password(@RequestBody PasswordInput input,HttpServletRequest request) {
        accounts.changePassword(id(request),input.oldPassword(),input.newPassword(),input.confirmPassword());
        return JsonResult.success(null,"密码已修改，请重新登录");
    }
    @GetMapping("/security") public Object security(HttpServletRequest request) {return JsonResult.success(accounts.security(id(request)));}
    @PutMapping("/security") public Object protection(@RequestBody ProtectionInput input,HttpServletRequest request) {
        if(input.enabled()==null)throw new IllegalArgumentException("请指定登录提醒状态");
        accounts.setProtection(id(request),input.enabled());return JsonResult.success(accounts.security(id(request)));
    }
    @GetMapping("/login-history") public Object history(HttpServletRequest request) {return JsonResult.success(accounts.loginHistory(id(request)));}
    @GetMapping("/alerts") public Object alerts(HttpServletRequest request) {return JsonResult.success(accounts.alerts(id(request)));}
    @PostMapping("/alerts/{alert}/read") public Object read(@PathVariable long alert,HttpServletRequest request) {accounts.readAlert(id(request),alert);return JsonResult.success();}
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object invalid(IllegalArgumentException error) {return JsonResult.error(400,error.getMessage());}
    @ExceptionHandler(IOException.class) @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object uploadFailed(IOException error) {return JsonResult.error(500,"头像保存失败，请稍后重试");}
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class) @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Object tooLarge() {return JsonResult.error(413,"头像大小不能超过2MB");}
}
