package com.sfs.usercenter.model.Requset;

import lombok.Data;

import java.io.Serializable;
/**
 * 登录请求实体
 *
 * @author 史方树
 * */
@Data
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = -2815635765513941027L;

    private String userAccount;

    private String UserPassword;
}
