package com.wangyu.shortlink.project.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wangyu.shortlink.project.common.convention.exception.ClientException;
import com.wangyu.shortlink.project.common.convention.exception.ServiceException;
import com.wangyu.shortlink.project.common.database.BaseDO;
import com.wangyu.shortlink.project.common.enums.VailDateTypeEnum;
import com.wangyu.shortlink.project.dao.entity.ShortLinkDO;
import com.wangyu.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.wangyu.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.wangyu.shortlink.project.dao.mapper.ShortLinkMapper;
import com.wangyu.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.wangyu.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.wangyu.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.wangyu.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.wangyu.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.wangyu.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.wangyu.shortlink.project.service.ShortLinkService;
import com.wangyu.shortlink.project.toolkit.HashUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Wrapper;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * 短链接接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
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
        ShortLinkGotoDO linkGotoDO = ShortLinkGotoDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(requestParam.getGid())
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(linkGotoDO);
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
                .fullShortUrl("http://"+shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
//        IPage<ShortLinkDO> resultPage = baseMapper.pageLink(requestParam);
//        return resultPage.convert(each -> {
//            ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
//            result.setDomain("http://" + result.getDomain());
//            return result;
//        });

        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(BaseDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getCreatedType);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each->BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid, count(*) as shortLinkCount")
                .in("gid", requestParam)
                .eq("enable_status", 0)
                .eq("del_flag", 0)
                .eq("del_time", 0L)
                .groupBy("gid");
        List<Map<String, Object>> shortLinkDOList = baseMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(shortLinkDOList, ShortLinkGroupCountQueryRespDTO.class);
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
//        verificationWhitelist(requestParam.getOriginUrl());
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(hasShortLinkDO.getDomain())
                .shortUri(hasShortLinkDO.getShortUri())
                .favicon(hasShortLinkDO.getFavicon())
                .createdType(hasShortLinkDO.getCreatedType())
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .describe(requestParam.getDescribe())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .build();
        if (hasShortLinkDO == null) {
            throw new ClientException("短链接记录不存在");
        }
        if (Objects.equals(hasShortLinkDO.getGid(), requestParam.getGid())) {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);
            baseMapper.update(shortLinkDO, updateWrapper);
        } else {
            // 为什么监控表要加上Gid？不加的话是否就不存在读写锁？详情查看：https://nageoffer.com/shortlink/question
//            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, requestParam.getFullShortUrl()));
//            RLock rLock = readWriteLock.writeLock();
//            rLock.lock();
//            try {
                LambdaUpdateWrapper<ShortLinkDO> linkUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getDelTime, 0L)
                        .eq(ShortLinkDO::getEnableStatus, 0);
                baseMapper.delete(linkUpdateWrapper);
                baseMapper.insert(shortLinkDO);
//                ShortLinkDO delShortLinkDO = ShortLinkDO.builder()
//                        .delTime(System.currentTimeMillis())
//                        .build();
//                delShortLinkDO.setDelFlag(1);
//                baseMapper.update(delShortLinkDO, linkUpdateWrapper);
//                ShortLinkDO shortLinkDO = ShortLinkDO.builder()
//                        .domain(createShortLinkDefaultDomain)
//                        .originUrl(requestParam.getOriginUrl())
//                        .gid(requestParam.getGid())
//                        .createdType(hasShortLinkDO.getCreatedType())
//                        .validDateType(requestParam.getValidDateType())
//                        .validDate(requestParam.getValidDate())
//                        .describe(requestParam.getDescribe())
//                        .shortUri(hasShortLinkDO.getShortUri())
//                        .enableStatus(hasShortLinkDO.getEnableStatus())
//                        .totalPv(hasShortLinkDO.getTotalPv())
//                        .totalUv(hasShortLinkDO.getTotalUv())
//                        .totalUip(hasShortLinkDO.getTotalUip())
//                        .fullShortUrl(hasShortLinkDO.getFullShortUrl())
//                        .favicon(getFavicon(requestParam.getOriginUrl()))
//                        .delTime(0L)
//                        .build();
//                baseMapper.insert(shortLinkDO);
//                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
//                        .eq(ShortLinkGotoDO::getFullShortUrl, requestParam.getFullShortUrl())
//                        .eq(ShortLinkGotoDO::getGid, hasShortLinkDO.getGid());
//                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
//                shortLinkGotoMapper.delete(linkGotoQueryWrapper);
//                shortLinkGotoDO.setGid(requestParam.getGid());
//                shortLinkGotoMapper.insert(shortLinkGotoDO);
//            } finally {
//                rLock.unlock();
//            }
        }
        // 短链接如何保障缓存和数据库一致性？详情查看：https://nageoffer.com/shortlink/question
//        if (!Objects.equals(hasShortLinkDO.getValidDateType(), requestParam.getValidDateType())
//                || !Objects.equals(hasShortLinkDO.getValidDate(), requestParam.getValidDate())
//                || !Objects.equals(hasShortLinkDO.getOriginUrl(), requestParam.getOriginUrl())) {
//            stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
//            Date currentDate = new Date();
//            if (hasShortLinkDO.getValidDate() != null && hasShortLinkDO.getValidDate().before(currentDate)) {
//                if (Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()) || requestParam.getValidDate().after(currentDate)) {
//                    stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
//                }
//            }
//        }
    }

    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        // 短链接接口的并发量有多少？如何测试？详情查看：https://nageoffer.com/shortlink/question
        // 面试中如何回答短链接是如何跳转长链接？详情查看：https://nageoffer.com/shortlink/question
        String serverName = request.getServerName();
        String serverPort = Optional.of(request.getServerPort())
                .filter(each -> !Objects.equals(each, 80))
                .map(String::valueOf)
                .map(each -> ":" + each)
                .orElse("");
        String fullShortUrl = serverName + "/" + shortUri;
//        String originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
//        if (StrUtil.isNotBlank(originalLink)) {
//            shortLinkStats(buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
//            ((HttpServletResponse) response).sendRedirect(originalLink);
//            return;
//        }
//        boolean contains = shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl);
//        if (!contains) {
//            ((HttpServletResponse) response).sendRedirect("/page/notfound");
//            return;
//        }
//        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
//        if (StrUtil.isNotBlank(gotoIsNullShortLink)) {
//            ((HttpServletResponse) response).sendRedirect("/page/notfound");
//            return;
//        }
//        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
//        lock.lock();
        try {
//            originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
//            if (StrUtil.isNotBlank(originalLink)) {
//                shortLinkStats(buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
//                ((HttpServletResponse) response).sendRedirect(originalLink);
//                return;
//            }
//            gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
//            if (StrUtil.isNotBlank(gotoIsNullShortLink)) {
//                ((HttpServletResponse) response).sendRedirect("/page/notfound");
//                return;
//            }
            LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
            if (shortLinkGotoDO == null) {
//                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
//                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
//            if (shortLinkDO == null || (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date()))) {
//                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
//                ((HttpServletResponse) response).sendRedirect("/page/notfound");
//                return;
//            }
//            stringRedisTemplate.opsForValue().set(
//                    String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
//                    shortLinkDO.getOriginUrl(),
//                    LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()), TimeUnit.MILLISECONDS
//            );
//            shortLinkStats(buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
            if(shortLinkDO!=null){
                ((HttpServletResponse) response).sendRedirect(shortLinkDO.getOriginUrl());
            }
        } finally {
//            lock.unlock();
        }
    }
}
