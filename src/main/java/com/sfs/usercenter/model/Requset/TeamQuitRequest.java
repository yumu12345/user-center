package com.sfs.usercenter.model.Requset;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = 6121121416581141777L;

    /**
     * id
     */
    private Long id;
}
