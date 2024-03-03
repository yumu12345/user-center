package com.sfs.usercenter.model.Requset;


import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求实体
 *
 * @author 史方树
 * */

@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = -5291302927750911175L;

    private String userAccount;

    private String UserPassword;

    private String checkPassword;

    private String planetCode;
}
