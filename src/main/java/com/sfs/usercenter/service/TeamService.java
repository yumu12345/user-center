package com.sfs.usercenter.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.sfs.usercenter.model.Requset.TeamJoinRequest;
import com.sfs.usercenter.model.Requset.TeamQuery;
import com.sfs.usercenter.model.Requset.TeamQuitRequest;
import com.sfs.usercenter.model.Requset.TeamUpdateRequest;
import com.sfs.usercenter.model.domain.Team;
import com.sfs.usercenter.model.domain.User;
import com.sfs.usercenter.model.vo.TeamUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 史方树
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-02-22 20:37:48
*/
public interface TeamService extends IService<Team> {

    Long addTeam(Team team, HttpServletRequest httpServletRequest);

    List<TeamUserVO> listTeam(TeamQuery teamQuery, HttpServletRequest request);

    boolean updateTeam(TeamUpdateRequest team, HttpServletRequest request);

    boolean joinTeam (TeamJoinRequest teamJoinRequest, HttpServletRequest request);

    Boolean quitTeam(TeamQuitRequest teamQuitRequest, HttpServletRequest request);

    Boolean deleteTeam(Long teamId,Long userId);

    Boolean captainDissolution(Long teamId,HttpServletRequest request);

    List<User> matchUser(long num, User loginUser);
}
