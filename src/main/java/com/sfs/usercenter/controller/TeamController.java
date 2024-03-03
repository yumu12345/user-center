package com.sfs.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sfs.usercenter.common.BaseResponse;
import com.sfs.usercenter.common.ErrorCode;
import com.sfs.usercenter.common.ResultUtils;
import com.sfs.usercenter.exception.BusinessException;
import com.sfs.usercenter.model.Requset.TeamJoinRequest;
import com.sfs.usercenter.model.Requset.TeamQuery;
import com.sfs.usercenter.model.Requset.TeamQuitRequest;
import com.sfs.usercenter.model.Requset.TeamUpdateRequest;
import com.sfs.usercenter.model.domain.Team;
import com.sfs.usercenter.model.domain.User;
import com.sfs.usercenter.model.domain.UserTeam;
import com.sfs.usercenter.model.vo.TeamUserVO;
import com.sfs.usercenter.service.TeamService;
import com.sfs.usercenter.service.UserService;
import com.sfs.usercenter.service.UserTeamService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
public class TeamController {
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody Team team, HttpServletRequest httpServletRequest){
        if (team==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍为空");
        }
        Long teamId = teamService.addTeam(team, httpServletRequest);
        return ResultUtils.success(teamId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody Long id){
        if (id<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍id错误");
        }
        boolean b = teamService.removeById(id);
        if (!b){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest team, HttpServletRequest request){
        if (team==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean update = teamService.updateTeam(team,request);
        if (!update){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping ("/get")
    public BaseResponse<Team> getTeam(@RequestParam long id){
        if (id<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍id错误");
        }
        Team team = teamService.getById(id);
        if (team==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"获取队伍信息失败");
        }
        return ResultUtils.success(team);
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeam(@RequestParam TeamQuery teamQuery, HttpServletRequest request){
        if (teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<TeamUserVO> teamList = teamService.listTeam(teamQuery,request);
        if (teamList==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"查找失败");
        }
        List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        Long myId = userService.getLoginUser(request).getId();
        QueryWrapper<UserTeam> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("userId",myId).in("teamId",teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        List<Long> joinTeamIdList = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toList());
        teamList.forEach(team->{
            boolean contains = joinTeamIdList.contains(team.getId());
            team.setHasJoin(contains);
        });
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId", teamIdList);
        List<UserTeam> mapUserTeamList = userTeamService.list(userTeamJoinQueryWrapper);
        // 队伍 id => 加入这个队伍的用户列表
        Map<Long, List<UserTeam>> teamIdUserTeamList = mapUserTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamList.forEach(team -> team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(), new ArrayList<>()).size()));
        return ResultUtils.success(teamList);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> PageTeam(@RequestParam TeamQuery teamQuery){
        if (teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(team,teamQuery);
        Page<Team> page=new Page<>(teamQuery.getPageNum(),teamQuery.getPageSize());
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>(team);
        Page<Team> result = teamService.page(page,teamQueryWrapper);
        if (result==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"查找失败");
        }
        return ResultUtils.success(result);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> join(@RequestBody TeamJoinRequest teamJoinRequest,HttpServletRequest request){
        if (teamJoinRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.joinTeam(teamJoinRequest, request);
        return ResultUtils.success(result);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest,HttpServletRequest request){
        if (teamQuitRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(teamService.quitTeam(teamQuitRequest,request));
    }

    @PostMapping("/dissolution")
    public BaseResponse<Boolean> captainDissolution(Long teamId,HttpServletRequest request){
        return ResultUtils.success(teamService.captainDissolution(teamId,request));
    }

    @GetMapping("/match")
    public BaseResponse<List<User>> matchUser(long num,HttpServletRequest request){
        if (num<=0||num>20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(teamService.matchUser(num,loginUser));
    }


}
