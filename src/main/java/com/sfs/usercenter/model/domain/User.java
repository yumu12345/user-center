package com.sfs.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户表
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * 用户id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 昵称
     */
    private String username;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像地址
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private Integer genter;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户状态（例如是否被封号）0是正常
     */
    private Integer userStatus;

    /**
     * 电话号码
     */
    private String phone;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否被删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 身份
     */
    private Integer userRole;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 星球编号
     */
    private String planetCode;

    /***
     * 标签列表
     */
    private String tags;


}