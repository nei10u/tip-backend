package com.nei10u.tip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nei10u.tip.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户Mapper (基于参考实现)
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据渠道关系ID查询用户
     */
    User getUserByRelationId(@Param("relationId") Long relationId);

    /**
     * 根据专用ID查询用户
     */
    User getUserBySpecialId(@Param("specialId") Long specialId);

    /**
     * 根据淘宝用户ID查询
     */
    User getUserByTbUserId(@Param("tbUserId") String tbUserId);

    /**
     * 根据UnionID查询
     */
    User getUserByUnionId(@Param("unionId") String unionId);

    /**
     * 根据手机号查询
     */
    User getUserByPhone(@Param("phone") String phone);

    /**
     * 根据拼多多PID查询
     */
    User getUserByPddPid(@Param("pddPid") String pddPid);

    /**
     * 根据京东授权/绑定ID查询
     */
    User getUserByJdAuthId(@Param("jdAuthId") String jdAuthId);

    /**
     * 根据Token查询
     */
    User getUserByToken(@Param("token") String token);

    /**
     * 根据小程序OpenID查询
     */
    User getUserByMnOpenId(@Param("mnOpenId") String mnOpenId);
}
