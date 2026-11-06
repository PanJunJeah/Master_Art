package com.pan.master_art.repository;

import com.pan.master_art.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查询用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查询用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 根据用户名或邮箱模糊查询
     */
    Page<User> findByUsernameContainingOrEmailContaining(String username, String email, Pageable pageable);
    
    /**
     * 根据角色查询用户
     */
    Page<User> findByRole(String role, Pageable pageable);
    
    /**
     * 根据状态查询用户
     */
    Page<User> findByStatus(Integer status, Pageable pageable);
}