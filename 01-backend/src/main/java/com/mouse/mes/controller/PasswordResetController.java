package com.mouse.mes.controller;

import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.service.PasswordResetService;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PasswordResetController {
    private final PasswordResetService resets;

    public PasswordResetController(PasswordResetService resets) {
        this.resets = resets;
    }

    @PostMapping("/api/users/{id}/password-reset")
    public JsonResult<String> issue(@PathVariable long id, HttpServletRequest request) {
        var actor = (LoginResponse) request.getAttribute("userInfo");
        if (actor == null || !Long.valueOf(9).equals(actor.getRoleId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (actor.getId() == id) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请从个人设置修改自己的密码");
        }
        return JsonResult.success(resets.issue(id));
    }

    @PostMapping("/api/auth/password-reset")
    public JsonResult<?> redeem(@RequestBody ResetInput input) {
        try {
            resets.redeem(input.userCode(), input.credential(), input.newPassword());
            return JsonResult.success();
        } catch (IllegalArgumentException e) {
            return JsonResult.error(400, e.getMessage());
        }
    }

    public record ResetInput(String userCode, String credential, String newPassword) {}
}
