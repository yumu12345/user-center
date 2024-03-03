package com.sfs.usercenter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.sfs.usercenter.Mapper")
//@ComponentScan(basePackages ={"com.sfs.usercenter.Mapper","com.sfs.usercenter.config"})
public class UserCenterApplication {

    //这是第一次使用分支
    public static void main(String[] args) {
        SpringApplication.run(UserCenterApplication.class, args);
    }

}
