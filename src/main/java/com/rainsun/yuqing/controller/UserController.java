package com.rainsun.yuqing.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rainsun.yuqing.common.BaseResponse;
import com.rainsun.yuqing.common.ErrorCode;
import com.rainsun.yuqing.constant.UserConstant;
import com.rainsun.yuqing.exception.BusinessException;
import com.rainsun.yuqing.model.domain.User;
import com.rainsun.yuqing.model.domain.request.UserLoginRequest;
import com.rainsun.yuqing.model.domain.request.UserRegisterRequest;
import com.rainsun.yuqing.service.UserService;
import com.rainsun.yuqing.utils.ResultUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 * @author rainsun
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        if(userRegisterRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if(StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)){
            throw new BusinessException(ErrorCode.DATA_NULL_ERROR, "参数不能为空");
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        if(userLoginRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if(StringUtils.isAnyBlank(userAccount, userPassword)){
            throw new BusinessException(ErrorCode.DATA_NULL_ERROR, "参数不能为空");
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request){
        if(request == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request){
        // todo:校验用户是否合法
        User userObject = (User)request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if(userObject == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN, "用户未登录");
        }
        User currentUser = userService.getById(userObject.getId());
        User saftetyUser = userService.getSaftetyUser(currentUser);
        return ResultUtils.success(saftetyUser);
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request){
        // 鉴权：仅管理员可查询
        if(!isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH, "非管理员用户");
        }

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(username)){
            wrapper.like("username", username);
        }
        List<User> userList = userService.list(wrapper);

        List<User> list = userList.stream().map(user -> userService.getSaftetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    @PostMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteUser(@PathVariable Long id, HttpServletRequest request){
        if(id < 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if(!isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH, "非管理员用户");
        }
        boolean result = userService.removeById(id);
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody User user, HttpServletRequest request){
        if(user == null || user.getId() < 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if(!isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH, "非管理员用户");
        }
        boolean result = userService.updateById(user);
        return ResultUtils.success(result);
    }

    /**
     * 是否为管理员
     * @param request
     * @return
     */
    public boolean isAdmin(HttpServletRequest request){
        // 鉴权：仅管理员可操作
        User userObject = (User)request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        return userObject != null && userObject.getUserRole() == UserConstant.ADMIN_ROLE;
    }
}
