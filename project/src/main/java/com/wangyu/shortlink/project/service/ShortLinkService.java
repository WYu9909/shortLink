package com.wangyu.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wangyu.shortlink.project.dao.entity.ShortLinkDO;
import com.wangyu.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.wangyu.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.wangyu.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.wangyu.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.wangyu.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.wangyu.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import com.wangyu.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.wangyu.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.wangyu.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.util.List;

/**
 * 短链接接口层
 */
public interface ShortLinkService extends IService<ShortLinkDO> {

    /**
     * 创建短链接
     *
     * @param requestParam 创建短链接请求参数
     * @return 短链接创建信息
     */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);

    /**
     * 分页查询短链接
     *
     * @param requestParam 分页查询短链接请求参数
     * @return 短链接分页返回结果
     */
    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);

    /**
     * 查询短链接分组内数量
     *
     * @param requestParam 查询短链接分组内数量请求参数
     * @return 查询短链接分组内数量响应
     */
    List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam);

    /**
     * 修改短链接
     *
     * @param requestParam 修改短链接请求参数
     */
    void updateShortLink(ShortLinkUpdateReqDTO requestParam);

    /**
     * 短链接跳转
     *
     * @param shortUri 短链接后缀
     * @param request  HTTP 请求
     * @param response HTTP 响应
     */
    void restoreUrl(String shortUri, ServletRequest request, ServletResponse response);

    /**
     * 短链接统计
     *
     * @param shortLinkStatsRecord 短链接统计实体参数
     */
    void shortLinkStats(String fullShortLink,String gid, ServletRequest request, ServletResponse servletResponse);

    /**
     * 批量创建短链接
     *
     * @param requestParam 批量创建短链接请求参数
     * @return 批量创建短链接返回参数
     */
    ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam);
}
