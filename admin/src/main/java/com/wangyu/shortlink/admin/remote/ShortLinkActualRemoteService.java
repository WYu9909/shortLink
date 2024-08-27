/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wangyu.shortlink.admin.remote;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wangyu.shortlink.admin.common.convention.result.Result;
import com.wangyu.shortlink.admin.dto.req.RecycleBinRecoverReqDTO;
import com.wangyu.shortlink.admin.dto.req.RecycleBinSaveReqDTO;
import com.wangyu.shortlink.admin.remote.dto.req.*;
import com.wangyu.shortlink.admin.remote.dto.resp.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 短链接中台远程调用服务
 */
//@FeignClient(
//        value = "short-link-project",
//        url = "${aggregation.remote-url:}",
//        configuration = OpenFeignConfiguration.class
//)
public interface ShortLinkActualRemoteService {

    default Result<ShortLinkCreateRespDTO> createShortLink(ShortLinkCreateReqDTO requestParam){
        String resultBodyStr = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/create", JSON.toJSONString(requestParam));
        String resultPageStr = resultBodyStr;
        return JSON.parseObject(resultPageStr, new TypeReference<>() {
        });
    }

    default Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){
        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("gid",requestParam.getGid());
        requestMap.put("orderTag", requestParam.getOrderTag());
        requestMap.put("current",requestParam.getCurrent());
        requestMap.put("size",requestParam.getSize());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/page", requestMap);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {
        });
    }


    default Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(List<String> requestParam){
        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("requestParam",requestParam);
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/count", requestMap);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {
        });
    }

    @PostMapping("/api/short-link/v1/update")
    default void updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam) {
        HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/update", JSON.toJSONString(requestParam));
    }

    /**
     * 根据 URL 获取标题
     *
     * @param url 目标网站地址
     * @return 网站标题
     */
//    @GetMapping("/api/short-link/v1/title")
    default Result<String> getTitleByUrl(@RequestParam("url") String url){
        String resultStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/title?url=" + url);
        return JSON.parseObject(resultStr, new TypeReference<>() {
        });
    };

    /**
     * 保存回收站
     *
     * @param requestParam 请求参数
     */
//    @PostMapping("/api/short-link/v1/recycle-bin/save")
    default void saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam){
        HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/save",JSON.toJSONString(requestParam));
    };

    /**
     * 分页查询回收站短链接
     *
     * @param gidList 分组标识集合
     * @param current 当前页
     * @param size    当前数据多少
     * @return 查询短链接响应
     */
//    @GetMapping("/api/short-link/v1/recycle-bin/page")
    default Result<Page<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam){
        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("gidList",requestParam.getGidList());
        requestMap.put("current",requestParam.getCurrent());
        requestMap.put("size",requestParam.getSize());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/page", requestMap);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {
        });

    }


    /**
     * 恢复短链接
     *
     * @param requestParam 短链接恢复请求参数
     */
//    @PostMapping("/api/short-link/v1/recycle-bin/recover")
    default void recoverRecycleBin(@RequestBody RecycleBinRecoverReqDTO requestParam){
        HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/recover",JSON.toJSONString(requestParam));
    }



    /**
     * 移除短链接
     *
     * @param requestParam 短链接移除请求参数
     */
//    @PostMapping("/api/short-link/v1/recycle-bin/remove")
    default void removeRecycleBin(@RequestBody RecycleBinRemoveReqDTO requestParam){
        HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/remove",JSON.toJSONString(requestParam));
    }

    /**
     * 访问单个短链接指定时间内监控访问记录数据
     *
     * @param requestParam 访问短链接监控访问记录请求参数
     * @return 短链接监控访问记录信息
     */
    default Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(requestParam, false, true);
        stringObjectMap.remove("orders");
        stringObjectMap.remove("records");
        String resultBodyStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/stats/access-record", stringObjectMap);
        return JSON.parseObject(resultBodyStr, new TypeReference<>() {
        });
    }

    default Result<ShortLinkStatsRespDTO> oneShortLinkStats(ShortLinkStatsReqDTO requestParam) {
        String resultBodyStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/stats", BeanUtil.beanToMap(requestParam));
        return JSON.parseObject(resultBodyStr, new TypeReference<>() {
        });
    }
}
