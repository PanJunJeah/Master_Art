package com.pan.master_art.service.impl;

import com.pan.master_art.entity.User;
import com.pan.master_art.repository.UserRepository;
import com.pan.master_art.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    

    
    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
    
    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    @Override
    @Transactional
    public User save(User user) {
        // 设置默认角色
        if (user.getRole() == null) {
            user.setRole("ROLE_USER");
        }
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public User update(User user) {
        User existingUser = findById(user.getId());
        if (existingUser == null) {
            return null;
        }
        
        // 不更新密码和创建时间
        user.setPassword(existingUser.getPassword());
        user.setCreateTime(existingUser.getCreateTime());
        
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
    
    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    @Override
    public Page<User> findByUsernameOrEmail(String keyword, Pageable pageable) {
        return userRepository.findByUsernameContainingOrEmailContaining(keyword, keyword, pageable);
    }
    
    @Override
    public Page<User> findByRole(String role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }
    
    @Override
    public Page<User> findByStatus(Integer status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable);
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Override
    @Transactional
    public User updateStatus(Long id, Integer status) {
        User user = findById(id);
        if (user == null) {
            return null;
        }
        user.setStatus(status);
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public User updateRole(Long id, String role) {
        User user = findById(id);
        if (user == null) {
            return null;
        }
        user.setRole(role);
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public User updatePassword(Long id, String newPassword) {
        User user = findById(id);
        if (user == null) {
            return null;
        }
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public void updateLastLoginTime(Long id) {
        User user = findById(id);
        if (user != null) {
            user.setLastLoginTime(new Date());
            userRepository.save(user);
        }
    }
    @Override
    @Transactional
    public User register(User user) {
        user.setCreateTime(new Date());
        userRepository.save(user);
        return user;
    }


    @Override
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        return false;
    }

    @Override
    public Page<User> findByUsername(String username, Pageable pageable) {
        return null;
    }
}