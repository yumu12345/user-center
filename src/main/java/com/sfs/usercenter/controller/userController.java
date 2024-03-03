package com.sfs.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sfs.usercenter.common.BaseResponse;
import com.sfs.usercenter.common.ErrorCode;

import com.sfs.usercenter.common.ResultUtils;
import com.sfs.usercenter.exception.BusinessException;
import com.sfs.usercenter.model.Requset.UserLoginRequest;
import com.sfs.usercenter.model.Requset.UserRegisterRequest;
import com.sfs.usercenter.model.domain.User;
import com.sfs.usercenter.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户接口
 *
 * @author 史方树
 * */
@RestController
@RequestMapping("/user")
public class userController {
    @Resource
    private UserService userService;


    @GetMapping("/current")
    public BaseResponse<User> getCurrent(HttpServletRequest request){
        Object object = request.getSession().getAttribute(UserService.USER_LOGIN_STATE);
        User userCurrent=(User) object;
        if (userCurrent==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        Long id = userCurrent.getId();
        User user = userService.getById(id);
        User selfUser = userService.getSelfUser(user);
        return ResultUtils.success(selfUser);
    }

    @PostMapping("/register")
    public BaseResponse<Long> userRegin(@RequestBody UserRegisterRequest userRegisterRequest){

        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode=userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword,planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(userService.userRegister(userAccount, userPassword, checkPassword,planetCode ));
    }

    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest,HttpServletRequest request){
        if (userLoginRequest == null) {
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        return ResultUtils.success(userService.doRegister(userAccount, userPassword, request));

    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request){
        if (request == null) {
            return null;
        }

        return userService.userLogout( request );
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username,HttpServletRequest request){
        Object object = request.getSession().getAttribute(UserService.USER_LOGIN_STATE);
        User user=(User)object;
        if (userService.isAdmin(user)){
            throw new BusinessException(ErrorCode.NO_AUTH,"无权限");
        }
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        if (!StringUtils.isAnyBlank(username)){
            queryWrapper.like("username",username);
        }
        return ResultUtils.success(userService.list(queryWrapper));
    }

    @PostMapping("/deleter")
    public BaseResponse<Boolean> deleterUser(@RequestBody long id,HttpServletRequest request){
        Object object = request.getSession().getAttribute(UserService.USER_LOGIN_STATE);
        User user=(User)object;
        if (userService.isAdmin(user)){
            throw new BusinessException(ErrorCode.NO_AUTH,"你不是管理员");
        }
        if (id<=0){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"系统错误");
        }
        return ResultUtils.success(userService.removeById(id));
    }

    @GetMapping("/searchByTags")
    public BaseResponse<List<User>> searchUserByTags( @RequestParam(required = false) List<String> tagNames){
        if (CollectionUtils.isEmpty(tagNames)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(userService.searchUsersByTagsBySql(tagNames));
    }

    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user,HttpServletRequest request){
        if (user==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数错误");
        }
        User loginUser=userService.getLoginUser(request);
        int result = userService.updateUser(user, loginUser);
        return ResultUtils.success(result);
    }

    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommend(Long size, Long pageNum, HttpServletRequest request){
        if (request==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户未登录");
        }
        return ResultUtils.success(userService.recommend(size,pageNum,request));
    }

}

