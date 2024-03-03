package com.sfs.usercenter.common;

import lombok.Data;

import java.io.Serializable;
@Data
public class PageRequest implements Serializable {
    private static final long serialVersionUID = -1387425651322610099L;
    /**
     * 页面大小
     */
    private int pageSize=10;
    /**
     * 当前页面
     */
    private int pageNum=1;

}
