package com.sfs.usercenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sfs.usercenter.model.domain.UserTeam;
import com.sfs.usercenter.service.UserTeamService;
import com.sfs.usercenter.Mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author 史方树
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2024-02-22 20:40:26
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




