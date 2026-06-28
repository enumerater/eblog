package com.enumerate.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enumerate.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
