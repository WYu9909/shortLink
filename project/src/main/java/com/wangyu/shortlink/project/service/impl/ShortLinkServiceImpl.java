package com.wangyu.shortlink.project.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wangyu.shortlink.project.common.convention.exception.ServiceException;
import com.wangyu.shortlink.project.dao.entity.ShortLinkDO;
import com.wangyu.shortlink.project.dao.mapper.ShortLinkMapper;
import com.wangyu.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.wangyu.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.wangyu.shortlink.project.service.ShortLinkService;
import com.wangyu.shortlink.project.toolkit.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.redisson.api.RBloomFilter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.sql.Wrapper;


/**
 * 短链接接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String shortLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl=StrBuilder
                .create(requestParam.getDomain())
                .append("/")
                .append(shortLinkSuffix)
                .toString();
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(requestParam.getDomain())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .shortUri(shortLinkSuffix)
                .enableStatus(0)
                .fullShortUrl(fullShortUrl)
                .build();
        try {
            baseMapper.insert(shortLinkDO);
        }catch (DuplicateKeyException ex){
            //TODO 已经误判的短链接如何处理
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl);
            ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
            if(hasShortLinkDO!=null){
                log.warn("短链接:{}重复入库",fullShortUrl);
                throw new ServiceException("短链接重复生成");
            }
        }
        shortUriCreateCachePenetrationBloomFilter.add(shortLinkSuffix);
        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl(shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    private String generateSuffix(ShortLinkCreateReqDTO requestParam){
        String shortUri;
        int customGenerateCount=0;
        while(true){
            if(customGenerateCount>10){
                throw new ServiceException("短链接频繁生成，请稍后再试");
            }
            String originUrl=requestParam.getOriginUrl();
            originUrl+=System.currentTimeMillis();
            shortUri = HashUtil.hashToBase62(originUrl);
            if(!shortUriCreateCachePenetrationBloomFilter.contains(requestParam.getDomain() + "/" + shortUri)){
                break;
            }
//            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
//                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getDomain() + "/" + shortUri);
//            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
//            if (shortLinkDO==null){
//                break;
//            }
            customGenerateCount++;
        }
        return shortUri;
    }
}
