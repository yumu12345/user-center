package com.sfs.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.reflect.TypeToken;
import com.sfs.usercenter.common.BaseResponse;
import com.sfs.usercenter.common.ErrorCode;
import com.sfs.usercenter.common.ResultUtils;
import com.sfs.usercenter.exception.BusinessException;
import com.sfs.usercenter.model.domain.User;
import com.sfs.usercenter.service.UserService;
import com.sfs.usercenter.Mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.google.gson.Gson;

/**
 * @author 史方树
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2024-01-21 18:25:17
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate redisTemplate;


    @Override
    public Long userRegister (String userAccount, String UserPassword, String checkPassword, String planetCode) {
        if (StringUtils.isAnyBlank(userAccount, UserPassword, checkPassword,planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (UserPassword.length() < 8) {
           throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if ( planetCode.length() > 5) {
           throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //账户不能含有特殊字符
        String regex = "[~`!@#\\$%\\^&\\*\\(\\)_\\-+=|{}':;',\\[\\]<>./?]";
        if (userAccount.matches(".*[" + regex + "].*")) {
           throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!UserPassword.equals(checkPassword)) {
           throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //账户不能相同
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
           throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //星球编码不能相同
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
           throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //加密
        final String SALT = "sfs";
        String newPassword=DigestUtils.md5DigestAsHex((SALT + UserPassword).getBytes());
        //插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(newPassword);
        user.setPlanetCode(planetCode);
        boolean result = this.save(user);
        if (!result) {
           throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return user.getId();
    }

    @Override
    public User doRegister(String userAccount, String UserPassword, HttpServletRequest request) {
        if (StringUtils.isAnyBlank(userAccount, UserPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (UserPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //账户不能含有特殊字符
        String regex = "[~`!@#\\$%\\^&\\*\\(\\)_\\-+=|{}':;',\\[\\]<>./?]";
        if (userAccount.matches(".*[" + regex + "].*")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //查询账户和密码
        final String SALT = "sfs";
        String newPassword = DigestUtils.md5DigestAsHex((SALT + UserPassword).getBytes());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", newPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("账号或密码错误");
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //数据脱敏
        User newUser = getSelfUser(user);

        //设置cookie
        request.getSession().setAttribute(USER_LOGIN_STATE,newUser);

        return newUser;

    }
    public User getSelfUser(User user){
        if (user==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User newUser =new User();
        newUser.setId(user.getId());
        newUser.setUsername(user.getUsername());
        newUser.setUserAccount(user.getUserAccount());
        newUser.setAvatarUrl(user.getAvatarUrl());
        newUser.setGenter(user.getGenter());
        newUser.setEmail(user.getEmail());
        newUser.setUserStatus(user.getUserStatus());
        newUser.setPhone(user.getPhone());
        newUser.setUserRole(user.getUserRole());
        newUser.setPlanetCode(user.getPlanetCode());
        newUser.setTags(user.getTags());
        return newUser;
    }

    @Override
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return ResultUtils.success(0);
    }

    @Override
    public List<User> searchUsersByTagsBySql(List<String> tagNames) {
//        if (CollectionUtils.isEmpty(tagNames)){
//           throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
//        for (String tagList : tagNames) {
//            queryWrapper = queryWrapper.like("tags", tagList);
//        }
//        List<User> userList = userMapper.selectList(queryWrapper);
//        return userList.stream().map(this::getSelfUser).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tagNames)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //拼接tag
        // like '%Java%' and like '%Python%'
        for (String tagList : tagNames) {
            queryWrapper = queryWrapper.like("tags", tagList);
        }
        List<User> userList = userMapper.selectList(queryWrapper);
        return  userList.stream().map(this::getSelfUser).collect(Collectors.toList());
    }

    @Override
    public List<User> searchUsersByTagsByMemory(List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        List<User> userList=userMapper.selectList(queryWrapper);
        Gson gson=new Gson();
        return userList.stream().filter(user -> {
            String tags=user.getTags();
            if (StringUtils.isAnyBlank(tags)){
                return false;
            }
            Set<String> tempTagNameSet = gson.fromJson(tags, new TypeToken<Set<String>>() {}.getType());
            for (String tag:tagNames){
                if (!tempTagNameSet.contains(tag)){
                    return false;
                }
            }
            return true;
        }).map(this::getSelfUser).collect(Collectors.toList());

    }

    /**
     * 获得当前用户信息
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request==null){
            return null;
        }
        Object object = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (object==null){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return (User) object;
    }

    @Override
    public int updateUser(User user, User loginUser) {
        Long userId = user.getId();
        if (userId<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //如果是管理员，允许更新任意用户信息
        //如果不是管理员，只允许更新自己的信息
        if (!isAdmin(loginUser)&&userId!=loginUser.getId()){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = userMapper.selectById(userId);
        if (oldUser==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return userMapper.updateById(user);
    }

    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser!=null&&loginUser.getUserRole()==0;
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object object = request.getSession().getAttribute(USER_LOGIN_STATE);
        User loginUser = (User) object;
        return loginUser!=null&&loginUser.getUserRole()==0;
    }

    @Override
    public Page<User> recommend(Long size, Long pageNum, HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        String redisKey = String.format("yumu::user::recommend:%s", loginUser.getId());
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Page<User> userPage =(Page<User>) valueOperations.get(redisKey);
        if (userPage!=null){
            return userPage;
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page pageB = new Page(pageNum, size);
        userPage = (Page<User>) page(pageB,queryWrapper);
        valueOperations.set(redisKey,userPage,300000, TimeUnit.MILLISECONDS);
        return userPage;
    }

}









