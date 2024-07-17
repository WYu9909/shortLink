package com.wangyu.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wangyu.shortlink.admin.dao.entity.UserDO;
import com.wangyu.shortlink.admin.dto.resp.UserRespDTO;

public interface UserService extends IService<UserDO> {
    UserRespDTO getUserByUsername(String username);

    /**
     * 查询用户名是否存在
     *
     * @param username 用户名
     * @return 用户名存在返回 True，不存在返回 False
     */
    Boolean hasUsername(String username);
}
