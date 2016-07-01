package com.gaohong.BIM;

import java.io.Serializable;

/**
 * @version 1.00.00
 * @description:
 * @author: archko 12-6-4
 */
public class FtpUser implements Serializable {

    /**
     * 用户名
     */
    public String username;
    /**
     * 密码
     */
    public String password;
    /**
     * 端口
     */
    public int port;
    /**
     * 主机
     */
    public String host;
}
