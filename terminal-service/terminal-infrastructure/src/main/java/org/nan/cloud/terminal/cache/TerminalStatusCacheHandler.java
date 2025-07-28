package org.nan.cloud.terminal.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.utils.BeanUtils;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.terminal.application.domain.TerminalStatusReport;
import org.nan.cloud.terminal.application.repository.TerminalReportRepository;
import org.nan.cloud.terminal.infrastructure.config.RedisConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class TerminalStatusCacheHandler {

    private final RedisTemplate redisTemplate;

    /**
     * 更新终端状态上报值缓存(led_status)
     */
    @SuppressWarnings("unchecked")
    public boolean tryUpdateTerminalStatusReport(Long oid, Long tid, TerminalStatusReport report) {
        Object o = redisTemplate.opsForValue().get(String.format(RedisConfig.RedisKeys.TERMINAL_STATUS_REPORT_PATTERN, oid, tid));
        if (Objects.nonNull(o)) {
            try {
                TerminalStatusReport origin = (TerminalStatusReport) o;
                BeanUtils.copyNonNullProperties(report, origin);
                redisTemplate.opsForValue().set(String.format(RedisConfig.RedisKeys.TERMINAL_STATUS_REPORT_PATTERN, oid, tid), origin, 30, TimeUnit.MINUTES);
                return true;
            } catch (Exception e) {
                log.warn("更新terminal status report缓存失败, tid:{}, report:{}, {}",tid, report, e.getMessage());
                return false;
            }
        }
        else return false;
    }

    @SuppressWarnings("unchecked")
    public void cacheTerminalStatusReport(Long oid, Long tid, TerminalStatusReport report) {
        redisTemplate.opsForValue().set(String.format(RedisConfig.RedisKeys.TERMINAL_STATUS_REPORT_PATTERN, oid, tid), report, 30, TimeUnit.MINUTES);
    }


}
