package com.mouse.mes.controller;

import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.mapper.UserMapper;
import com.mouse.mes.service.AccountService;
import com.mouse.mes.service.FaceLoginService;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;

@RestController
@ConditionalOnProperty(name = "mes.face.enabled", havingValue = "true")
public class FaceLoginController {
    private final FaceLoginService faces;
    private final AuthController auth;
    private final UserMapper users;
    private final AccountService accounts;

    public FaceLoginController(FaceLoginService faces, AuthController auth, UserMapper users, AccountService accounts) {
        this.faces = faces;
        this.auth = auth;
        this.users = users;
        this.accounts = accounts;
    }

    @PostMapping("/api/auth/face/challenge")
    public JsonResult<UUID> loginChallenge() {
        return JsonResult.success(faces.challenge(null));
    }

    @PostMapping("/api/auth/face/login")
    public JsonResult<LoginResponse> login(@RequestBody Capture input, HttpServletRequest request) {
        try {
            var match = faces.identify(input.challenge(), input.frames());
            if (match.isEmpty()) {
                return JsonResult.error(400, "人脸未匹配，请重试或使用密码");
            }
            long id = match.getAsLong();
            long version = accounts.sessionVersion(id);
            var user = users.findById(id);
            if (user == null || user.getPassword() == null || user.getPassword().isBlank()) {
                return JsonResult.error(400, "请先完成密码设置");
            }
            return auth.issueSession(user, version, request, "人脸");
        } catch (HttpServerErrorException.ServiceUnavailable e) {
            return JsonResult.error(503, "人脸服务繁忙，请稍后重新采集");
        } catch (RestClientException | GeneralSecurityException e) {
            return JsonResult.error(503, "人脸服务暂不可用，请使用密码");
        } catch (IllegalArgumentException e) {
            return JsonResult.error(400, e.getMessage());
        }
    }

    @PostMapping("/api/account/face/challenge")
    public JsonResult<UUID> enrollmentChallenge(HttpServletRequest request) {
        return JsonResult.success(faces.challenge(employee(request)));
    }

    @GetMapping("/api/account/face")
    public JsonResult<BindingStatus> status(HttpServletRequest request) {
        return JsonResult.success(new BindingStatus(faces.enrolledAt(employee(request))));
    }

    public record BindingStatus(String enrolledAt) {}

    @PutMapping("/api/account/face")
    public JsonResult<?> enroll(@RequestBody Capture input, HttpServletRequest request) {
        try {
            faces.enroll(employee(request), input.password(), input.challenge(), input.frames());
            return JsonResult.success();
        } catch (HttpServerErrorException.ServiceUnavailable e) {
            return JsonResult.error(503, "人脸服务繁忙，请稍后重新采集");
        } catch (RestClientException | GeneralSecurityException e) {
            return JsonResult.error(503, "人脸服务暂不可用，请使用密码");
        } catch (IllegalArgumentException e) {
            return JsonResult.error(400, e.getMessage());
        }
    }

    @PostMapping("/api/account/face/remove")
    public JsonResult<?> remove(@RequestBody PasswordInput input, HttpServletRequest request) {
        try {
            faces.remove(employee(request), input.password());
            return JsonResult.success();
        } catch (IllegalArgumentException e) {
            return JsonResult.error(400, e.getMessage());
        }
    }

    private long employee(HttpServletRequest request) {
        return ((LoginResponse) request.getAttribute("userInfo")).getId();
    }

    public record Capture(UUID challenge, List<String> frames, String password) {}
    public record PasswordInput(String password) {}
}
