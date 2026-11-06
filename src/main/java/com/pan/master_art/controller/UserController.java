package com.pan.master_art.controller;

import com.pan.master_art.entity.User;
import com.pan.master_art.service.UserService;
import com.pan.master_art.utils.JwtTokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
//@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        // ... 检查用户名和邮箱逻辑不变

        // 创建新用户
        User newUser = userService.register(user);

        // 生成 JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", newUser.getId());
        claims.put("role", newUser.getRole());
        String token = JwtTokenGenerator.generateToken(newUser.getUsername(), claims);

        // 返回 token 和用户信息
        Map<String, Object> response = new HashMap<>();
        response.put("message", "注册成功");
        response.put("user", newUser);
        response.put("token", token); // 添加 token

        return ResponseEntity.ok(response);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        String role = loginRequest.get("role");

        // 获取用户信息
        User user = userService.findByUsername(username);

        if (user != null) {
            if(Objects.equals(user.getPassword(), password)&&Objects.equals(user.getRole(), role)) {
                Map<String, Object> response = new HashMap<>();

                Map<String, Object> claims = new HashMap<>();
                claims.put("userId", user.getId());
                String token = JwtTokenGenerator.generateToken(
                        user.getUsername(),
                        claims);

                response.put("token", token);
                response.put("user", user);
                return ResponseEntity.ok(response);
            }
        }
        //提示登录失败
        Map<String, Object> response = new HashMap<>();
        response.put("原因","账号或者密码错误");
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(String username) {
        User user = userService.findByUsername(username);
        
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        User updatedUser = userService.update(user);
        
        if (updatedUser != null) {
            return ResponseEntity.ok(updatedUser);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordRequest) {
        String username = passwordRequest.get("username");
        String oldPassword = passwordRequest.get("oldPassword");
        String newPassword = passwordRequest.get("newPassword");
        
        boolean success = userService.changePassword(username, oldPassword, newPassword);
        
        if (success) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "密码修改成功");
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("message", "原密码不正确");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 管理员获取用户列表
     */
    @GetMapping("/getAllUsers")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Pageable pegeable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = Sort.by(direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<User> userPage;
        if (username != null) {
            userPage = userService.findByUsername(username, pageable);
        } else {
            userPage = userService.findAll(pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", userPage.getContent());
        response.put("currentPage", userPage.getNumber());
        response.put("totalItems", userPage.getTotalElements());
        response.put("totalPages", userPage.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 管理员更新用户状态
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        
        User user = userService.updateStatus(id, status);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}