package com.rainsun.yuqing.service;

import com.rainsun.yuqing.constant.UserConstant;
import com.rainsun.yuqing.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * @author rainsun
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2024-01-22 16:51:58
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 检查密码
     * @param planetCode 星球编号
     * @return 是否注册成功
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     * @param originUser
     * @return
     */
    User getSaftetyUser(User originUser);

    /**
     * 用户注销
     */
    int userLogout(HttpServletRequest request);

    List<User> searchUserByTags(List<String> tagNameList);

    /**
     * 更新用户信息
     * @param user
     * @return
     */
    Integer updateUser(User user, User loginUser);

    /**
     * 获取登录了的用户信息
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    boolean isAdmin(User loginUser);

    /**
     * 是否为管理员
     * @param request
     * @return
     */
    public boolean isAdmin(HttpServletRequest request);
}
