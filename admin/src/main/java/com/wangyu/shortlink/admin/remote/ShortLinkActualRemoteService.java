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

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wangyu.shortlink.admin.common.convention.result.Result;
import com.wangyu.shortlink.admin.dto.req.RecycleBinSaveReqDTO;
import com.wangyu.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.wangyu.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.wangyu.shortlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import com.wangyu.shortlink.admin.remote.dto.resp.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;

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

}
