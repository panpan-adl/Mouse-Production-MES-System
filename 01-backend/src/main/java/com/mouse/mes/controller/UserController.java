package com.mouse.mes.controller;

import com.mouse.mes.entity.User;
import com.mouse.mes.service.UserService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired private com.mouse.mes.service.EmployeeHistoryService employeeHistory;

    @Value("${upload.path:uploads/avatars}")
    private String uploadPath;

    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @GetMapping("/username/{username}")
    public User getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username);
    }

    @GetMapping("/usercode/{userCode}")
    public User getUserByUserCode(@PathVariable String userCode) {
        return userService.findByUserCode(userCode);
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.create(user);
    }

    @org.springframework.transaction.annotation.Transactional
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        User before=userService.findById(id);
        if(before==null)throw new IllegalArgumentException("员工不存在");
        user.setId(id);
        User after=userService.update(user);
        employeeHistory.record(before,after);
        return after;
    }

    @GetMapping("/{id}/history")
    public Object employeeHistory(@PathVariable Long id){return employeeHistory.history(id);}

    @DeleteMapping("/{id}")
    public JsonResult<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return JsonResult.success();
    }

    @GetMapping("/role/{roleCode}")
    public List<User> getUsersByRoleCode(@PathVariable String roleCode) {
        return userService.findByRoleCode(roleCode);
    }

    @GetMapping("/dept/{deptId}")
    public List<User> getUsersByDeptId(@PathVariable Long deptId) {
        return userService.findByDeptId(deptId);
    }

    @GetMapping("/all")
    public List<User> getAllUsersIncludingDeleted() {
        return userService.findAllIncludingDeleted();
    }

    @GetMapping("/deleted")
    public List<User> getDeletedUsers() {
        return userService.findDeletedUsers();
    }

    @PostMapping("/{id}/leave")
    public JsonResult<Void> employeeLeave(@PathVariable Long id) {
        userService.employeeLeave(id);
        return JsonResult.success();
    }

    @PostMapping("/{id}/restore")
    public JsonResult<User> restoreUser(@PathVariable Long id) {
        return JsonResult.success(userService.restoreUser(id));
    }

    @PostMapping("/{id}/avatar")
    public JsonResult<String> uploadAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return JsonResult.error("请选择上传文件");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.matches(".*\\.(jpg|jpeg|png|gif)$")) {
            return JsonResult.error("只支持JPG、PNG、GIF格式的图片");
        }

        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadDir.resolve(filename);

            try (java.io.InputStream input = file.getInputStream()) {
                Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            String avatarUrl = "/api/users/avatar/" + filename;
            userService.updateAvatar(id, avatarUrl);

            return JsonResult.success(avatarUrl);
        } catch (IOException e) {
            return JsonResult.error("上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/avatar/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        try {
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path filePath = uploadDir.resolve(filename).normalize();
            if (Paths.get(filename).isAbsolute() || !filePath.startsWith(uploadDir)) {
                return ResponseEntity.badRequest().build();
            }
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                return ResponseEntity.ok()
                        .contentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
