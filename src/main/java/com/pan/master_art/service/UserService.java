package com.pan.master_art.service;

import com.pan.master_art.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 根据ID查询用户
     */
    User findById(Long id);
    
    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);
    
    /**
     * 根据邮箱查询用户
     */
    User findByEmail(String email);
    
    /**
     * 保存用户
     */
    User save(User user);
    
    /**
     * 更新用户
     */
    User update(User user);
    
    /**
     * 删除用户
     */
    void delete(Long id);
    
    /**
     * 分页查询所有用户
     */
    Page<User> findAll(Pageable pageable);
    
    /**
     * 根据用户名或邮箱模糊查询
     */
    Page<User> findByUsernameOrEmail(String keyword, Pageable pageable);
    
    /**
     * 根据角色查询用户
     */
    Page<User> findByRole(String role, Pageable pageable);
    
    /**
     * 根据状态查询用户
     */
    Page<User> findByStatus(Integer status, Pageable pageable);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 更新用户状态
     */
    User updateStatus(Long id, Integer status);
    
    /**
     * 更新用户角色
     */
    User updateRole(Long id, String role);
    
    /**
     * 更新用户密码
     */
    User updatePassword(Long id, String newPassword);
    
    /**
     * 更新用户最后登录时间
     */
    void updateLastLoginTime(Long id);

    User register(User user);


    boolean changePassword(String username, String oldPassword, String newPassword);

    Page<User> findByUsername(String username, Pageable pageable);
}