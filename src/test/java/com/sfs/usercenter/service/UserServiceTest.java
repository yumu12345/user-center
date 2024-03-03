package com.sfs.usercenter.service;

import com.sfs.usercenter.model.domain.User;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class UserServiceTest {
    @Autowired
    private UserService userService;
    @Test
    public void text1(){
        List<String> tag= Arrays.asList("大一","男");
        List<User> userList = userService.searchUsersByTagsBySql(tag);
        Assert.assertNotNull(userList);
    }
}
