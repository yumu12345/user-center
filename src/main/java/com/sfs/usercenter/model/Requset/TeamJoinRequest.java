package com.sfs.usercenter.model.Requset;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamJoinRequest implements Serializable {
    private static final long serialVersionUID = 2924975724758161495L;

    /**
     * id
     */
    private long id;

    /**
     * 密码
     */
    private String password;
}
