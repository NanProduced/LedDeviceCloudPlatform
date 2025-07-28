package org.nan.cloud.terminal.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.utils.StringUtils;
import org.nan.cloud.terminal.infrastructure.config.RedisConfig;
import org.nan.cloud.terminal.infrastructure.persistence.mysql.entity.TerminalInfoDO;
import org.nan.cloud.terminal.infrastructure.persistence.mysql.mapper.TerminalInfoMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * websocket连接相关上下文信息缓存类
 * @author Nan
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebsocketConnectionCacheHandler {

    private final StringRedisTemplate stringRedisTemplate;

    private final TerminalInfoMapper terminalInfoMapper;

    /**
     * 缓存tid和oid的映射关系，防止移除websocket连接时遍历连接分片
     * @param tid
     * @return
     */
    public Long getOidByTid(Long tid) {
        String key = String.format(RedisConfig.RedisKeys.TID_TO_OID_PATTERN, tid);
        String s = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(s)) return Long.valueOf(s);
        else {
            Long oid = terminalInfoMapper.selectOne(new LambdaQueryWrapper<TerminalInfoDO>()
                    .select(TerminalInfoDO::getOid)
                    .eq(TerminalInfoDO::getTid, tid)).getOid();
            setOidToTidMap(oid, tid);
            return oid;
        }
    }

    public void setOidToTidMap(Long oid, Long tid) {
        String key = String.format(RedisConfig.RedisKeys.TID_TO_OID_PATTERN, tid);
        stringRedisTemplate.opsForValue().set(key, oid.toString(), 10, TimeUnit.MINUTES);
    }
}
