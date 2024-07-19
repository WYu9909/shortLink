package com.wangyu.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wangyu.shortlink.admin.dao.entity.UserDO;
import com.wangyu.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.wangyu.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.wangyu.shortlink.admin.dto.resp.UserRespDTO;
import org.springframework.web.bind.annotation.RequestBody;

public interface UserService extends IService<UserDO> {
    UserRespDTO getUserByUsername(String username);

    /**
     * 查询用户名是否存在
     *
     * @param username 用户名
     * @return 用户名存在返回 True，不存在返回 False
     */
    Boolean hasUsername(String username);

    /**
     * 注册用户
     *
     * @param requestParam 注册用户请求参数
     */
    void register(UserRegisterReqDTO requestParam);

    /**
     * 根据用户名修改用户
     *
     * @param requestParam 修改用户请求参数
     */
    void update(UserUpdateReqDTO requestParam);
}
