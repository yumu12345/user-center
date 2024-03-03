package com.sfs.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sfs.usercenter.common.ErrorCode;
import com.sfs.usercenter.exception.BusinessException;
import com.sfs.usercenter.model.Requset.TeamJoinRequest;
import com.sfs.usercenter.model.Requset.TeamQuery;
import com.sfs.usercenter.model.Requset.TeamQuitRequest;
import com.sfs.usercenter.model.Requset.TeamUpdateRequest;
import com.sfs.usercenter.model.domain.Team;
import com.sfs.usercenter.model.domain.TeamStatusEnum;
import com.sfs.usercenter.model.domain.User;
import com.sfs.usercenter.model.domain.UserTeam;
import com.sfs.usercenter.model.vo.TeamUserVO;
import com.sfs.usercenter.model.vo.UserVO;
import com.sfs.usercenter.service.UserService;
import com.sfs.usercenter.service.UserTeamService;
import com.sfs.usercenter.service.TeamService;
import com.sfs.usercenter.Mapper.TeamMapper;
import com.sfs.usercenter.utils.AlgorithmUtils;
import io.swagger.models.auth.In;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author 史方树
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2024-02-22 20:37:48
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserService userService;

    @Resource
    private UserTeamService userTeamService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long addTeam(Team team, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NO_AUTH,"请登录");
        }
        Integer maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum <1|| maxNum >20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"人数不合法");
        }
        String teamName = team.getName();
        if (StringUtils.isBlank(teamName)||teamName.length()>20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"标题过长");
        }
        String teamDescription = team.getDescription();
        if (StringUtils.isBlank(teamDescription)||teamDescription.length()>512){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"描述过长");
        }
        Integer teamStatus = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(teamStatus);
        if (enumByValue==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍状态不满足要求");
        }
        String teamPassword = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(enumByValue)){
            if (StringUtils.isBlank(teamPassword)||teamPassword.length()>32){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码不符合要求");
            }
        }
        if (new Date().after(team.getExpireTime())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"超出时间");
        }
        Long userId = loginUser.getId();
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("userId",userId);
        long count = count(teamQueryWrapper);
        if (count>5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"创建队伍名额已满，无法创建");
        }


        team.setId(null);
        team.setUserId(userId);
        boolean result = save(team);
        Long teamId = team.getId();
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"插入失败");
        }

        UserTeam userTeam=new UserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(userId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"插入失败");
        }
        return teamId;
    }

    @Override
    public List<TeamUserVO> listTeam(TeamQuery teamQuery, HttpServletRequest request) {

        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        if (teamQuery!=null) {


            String teamName = teamQuery.getName();
            if (!StringUtils.isBlank(teamName)) {
                teamQueryWrapper.eq("name", teamName);
            }

            String description = teamQuery.getDescription();
            if (!StringUtils.isBlank(description)) {
                teamQueryWrapper.eq("description", description);
            }

            String searchText = teamQuery.getSearchText();
            if (!StringUtils.isBlank(searchText)) {
                teamQueryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }

            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                teamQueryWrapper.eq("id", id);
            }

            List<Long> idList = teamQuery.getIdList();
            if (idList != null) {
                teamQueryWrapper.in("id", idList);
            }

            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum!=null&&maxNum>0){
                teamQueryWrapper.eq("maxNum",maxNum);
            }

            // 根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            boolean admin = userService.isAdmin(request);
            if (!admin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            teamQueryWrapper.eq("status", statusEnum.getValue());

        }

        //不能展示已经过期了的队伍
        teamQueryWrapper.and(qw->qw.gt("expireTime",new Date()).or().isNull("expireTime"));
        List<Team> teamList = list(teamQueryWrapper);
        if (CollectionUtils.isEmpty(teamList)){
            return new ArrayList<>();
        }

        List<TeamUserVO> listTeamUserVOS = new ArrayList<>();
        for (Team team:teamList){
            Long userId = team.getUserId();
            if (userId==null){
                continue;
            }
            User createrUser = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);

            //数据脱敏
            if (createrUser!=null){
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(createrUser,userVO);
                teamUserVO.setCreateUser(userVO);
            }

            listTeamUserVOS.add(teamUserVO);
        }
        return listTeamUserVOS;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest team, HttpServletRequest request) {
//        1. 判断请求参数是否为空
        if (team==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        2. 查询队伍是否存在
        Long id = team.getId();
        if(id==null || id < 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        3. 只有管理员或者队伍的创建者可以修改
        User loginUser = userService.getLoginUser(request);
        boolean admin = userService.isAdmin(request);
        Team oldTeam = getById(id);
        if (oldTeam.getUserId()!=loginUser.getId()&&admin){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
//        4. 如果用户传入的新值和老值一致，就不用 update 了（可自行实现，降低数据库使用次数）TODO
//        5. 如果队伍状态改为加密，必须要有密码
        Integer status = team.getStatus();
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
        String teamPassword = team.getPassword();
        if (!enumByValue.equals(TeamStatusEnum.SECRET)&&StringUtils.isBlank(teamPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        6. 更新成功
        Team updateTeam = new Team();
        BeanUtils.copyProperties(team,updateTeam);
        boolean result = updateById(updateTeam);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean joinTeam (TeamJoinRequest teamJoinRequest, HttpServletRequest request)  {
        User loginUser = userService.getLoginUser(request);
        if (loginUser==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        1. 用户最多加入 5 个队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        Long userId = loginUser.getId();
        userTeamQueryWrapper.eq("userId",userId);
        long count = userTeamService.count(userTeamQueryWrapper);
        if (count>5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"创建和加入队伍过多");
        }

//        2. 队伍必须存在，只能加入未满、未过期的队伍
        Long id = teamJoinRequest.getId();
        if (id==null||id<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = getById(id);
        if (team==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍不存在");
        }
        userTeamQueryWrapper=new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId",id);
        long count1 = userTeamService.count(userTeamQueryWrapper);
        if (team.getMaxNum()<=count1){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"人数已满，无法加入");
        }
        Date expireTime = team.getExpireTime();
        if (expireTime!=null&&expireTime.before(new Date())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"日期过期");
        }
         //该用户已加入的队伍数量
        userTeamQueryWrapper=new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId",userId);
        long count3 = userTeamService.count();
        if (count3>5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"加入队伍已满");
        }
//        3. 不能加入自己的队伍，不能重复加入已加入的队伍（幂等性）
        userTeamQueryWrapper=new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId",userId);
        userTeamQueryWrapper.eq("teamId",id);
        long count2 = userTeamService.count(userTeamQueryWrapper);
        if (count2>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"你已经加入队伍");
        }
//        4. 禁止加入私有的队伍
        Integer status = team.getStatus();
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
        if (enumByValue.equals(TeamStatusEnum.PRIVATE)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"无法加入私密队伍");
        }
//        5. 如果加入的队伍是加密的，必须密码匹配才可以
        if (enumByValue.equals(TeamStatusEnum.SECRET)){
            if (!StringUtils.isBlank(team.getPassword())){
                String teamPassword = teamJoinRequest.getPassword();
                if (!teamPassword.equals(team.getPassword())){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码不正确");
                }
            }
        }
        //        7. 新增队伍 - 用户关联信息
        UserTeam userTeam=new UserTeam();
        userTeam.setTeamId(id);
        userTeam.setUserId(userId);
        userTeam.setCreateTime(new Date());
        boolean result = userTeamService.save(userTeam);
        return result;
    }

    @Override
    public Boolean quitTeam(TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
//        1.  校验请求参数
        if (teamQuitRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        2.  校验队伍是否存在
        Long teamId = teamQuitRequest.getId();
        if (teamId==null || teamId<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team==null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
//        3.  校验我是否已加入队伍
        User loginUser = userService.getLoginUser(request);
        if (loginUser==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Long userId = loginUser.getId();
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId",userId);
        userTeamQueryWrapper.eq("teamId",teamId);
        long count = userTeamService.count(userTeamQueryWrapper);
        if (count<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"不在队伍");
        }
//        4.  如果队伍
//        a.  只剩一人，队伍解散

        userTeamQueryWrapper=new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId",userId);
        long count1 = userTeamService.count(userTeamQueryWrapper);
        if (count1==1){
            Boolean result = deleteTeam(teamId, userId);
            if(!result){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新队长失败");
            }
        }
//        b.  还有其他人
        if (count1>1){
            Long captainId = team.getUserId();
            //ⅰ.  如果是队长退出队伍，权限转移给第二早加入的用户 —— 先来后到 只用取 id 最小的 2 条数据
            if (captainId==userId){
                QueryWrapper<UserTeam> queryWrapper=new QueryWrapper<>();
                queryWrapper.eq("teamId",teamId);
                queryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList=userTeamService.list(queryWrapper);
                UserTeam newCaptain = userTeamList.get(1);
                Long  newCaptainId= newCaptain.getUserId();
                //更新当前队长
                Team updateTeam=new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(newCaptainId);
                boolean result = this.updateById(updateTeam);
                if(!result){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新队长失败");
                }
            }
            //ⅱ.  非队长，自己退出队伍
            QueryWrapper<UserTeam> queryWrapper=new QueryWrapper<>();
            queryWrapper.eq("userId",userId);
            queryWrapper.eq("teamId",teamId);
            boolean result = userTeamService.remove(userTeamQueryWrapper);
            if(!result){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
            }
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteTeam(Long teamId,Long userId ) {
        if (teamId==null||teamId<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = this.removeById(teamId);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
        }
        QueryWrapper<UserTeam> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        queryWrapper.eq("teamId",teamId);
        result  = userTeamService.remove(queryWrapper);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
        }
        return true;
    }

    @Override
    public Boolean captainDissolution(Long teamId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        Team team = this.getById(teamId);
        if (team==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long captainId = team.getUserId();
        if (captainId!=userId){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"你不是队长");
        }
        Boolean result = this.deleteTeam(teamId, userId);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
        }
        return true;
    }

    @Override
    public List<User> matchUser(long num, User loginUser) {

        //排除无用信息
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("tags");
        queryWrapper.select("id","tags");
        List<User> userList = userService.list(queryWrapper);

        String myTags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> myTagList = gson.fromJson(myTags, new TypeToken<List<String>>() {
        }.getType());

        List<Pair<User,Long>> list=new ArrayList<>();
        for (int i=0;i<userList.size();i++){
            User user = userList.get(i);
            String tags = user.getTags();
            if (tags==null){
                continue;
            }
            List<String> targetTag = gson.fromJson(tags, new TypeToken<List<String>>() {
            }.getType());
            long score = AlgorithmUtils.minDistance(myTagList, targetTag);
            list.add(new Pair<>(user,score));
        }
        List<Pair<User,Long>> topUserPairList=list.stream()
                                                  .sorted((a,b)->(int) (a.getValue()-b.getValue())).limit(num)
                                                  .collect(Collectors.toList());
        //有顺序的userID列表
        List<Long> listId=topUserPairList.stream().map(pair->pair.getKey().getId()).collect(Collectors.toList());

//        根据id查询user完整信息
        queryWrapper=new QueryWrapper<>();
        queryWrapper.in("id",listId);
        Map<Long,List<User>> userIdList=userService.list(queryWrapper)
                .stream().map(user->userService.getSelfUser(user))
                .collect(Collectors.groupingBy(User::getId));
        // 因为上面查询打乱了顺序，这里根据上面有序的userID列表赋值
        List<User> finalUserList=new ArrayList<>();
        for (Long userId :listId) {
            finalUserList.add(userIdList.get(userId).get(0));
        }
        return finalUserList;
    }
}




