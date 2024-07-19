package com.wangyu.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wangyu.shortlink.admin.dao.entity.GroupDO;

public interface GroupService extends IService<GroupDO> {
    /**
     * 新增短链接分组
     *
     * @param username  用户名
     * @param groupName 短链接分组名
     */
    void saveGroup(String groupName);
}
