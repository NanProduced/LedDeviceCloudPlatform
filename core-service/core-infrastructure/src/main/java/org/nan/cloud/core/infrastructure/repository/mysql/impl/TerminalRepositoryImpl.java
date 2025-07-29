package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.Terminal;
import org.nan.cloud.core.domain.TerminalAccount;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TerminalAccountDO;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TerminalInfoDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.TerminalConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.TerminalAccountMapper;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.TerminalInfoMapper;
import org.nan.cloud.core.repository.TerminalRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class TerminalRepositoryImpl implements TerminalRepository {

    private final TerminalAccountMapper terminalAccountMapper;

    private final TerminalInfoMapper terminalInfoMapper;

    private final TerminalConverter  terminalConverter;

    @Override
    public Long createTerminalAccount(TerminalAccount terminalAccount) {
        TerminalAccountDO terminalAccountDO = terminalConverter.toTerminalAccountDO(terminalAccount);
        terminalAccountDO.setStatus(0);
        terminalAccountDO.setCreateTime(LocalDateTime.now());
        terminalAccountMapper.insert(terminalAccountDO);
        return terminalAccountDO.getTid();
    }

    @Override
    public void createTerminal(Terminal terminal) {
        TerminalInfoDO  terminalInfoDO = terminalConverter.toTerminalInfoDO(terminal);
        terminalInfoDO.setCreatedAt(LocalDateTime.now());
        terminalInfoMapper.insert(terminalInfoDO);
    }

    @Override
    public PageVO<Terminal> pageTerminals(int pageNum, int pageSize, Long oid, Set<Long> tgids, String keyword, String terminalModel, Integer onlineStatus) {
        Page<TerminalInfoDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TerminalInfoDO> queryWrapper = new LambdaQueryWrapper<TerminalInfoDO>()
                .eq(TerminalInfoDO::getOid, oid)
                .in(TerminalInfoDO::getTgid, tgids);
        
        // 关键字搜索：终端名称或描述
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(qw -> qw
                    .like(TerminalInfoDO::getTerminalName, keyword)
                    .or()
                    .like(TerminalInfoDO::getDescription, keyword)
            );
        }
        
        // 终端型号筛选
        if (StringUtils.isNotBlank(terminalModel)) {
            queryWrapper.eq(TerminalInfoDO::getTerminalModel, terminalModel);
        }
        
        // TODO: 终端在线状态筛选 - 需要实现在线状态查询逻辑
        // if (Objects.nonNull(onlineStatus)) {
        //     // 在线状态不存在于MySQL中，需要从其他数据源查询
        // }
        
        queryWrapper.orderByDesc(TerminalInfoDO::getCreatedAt);

        IPage<TerminalInfoDO> pageResult = terminalInfoMapper.selectPage(page, queryWrapper);

        PageVO<Terminal> pageVO = PageVO.<Terminal>builder()
                .pageNum((int) pageResult.getCurrent())
                .pageSize((int) pageResult.getSize())
                .total(pageResult.getTotal())
                .records(terminalConverter.toTerminalList(pageResult.getRecords()))
                .build();
        pageVO.calculate();
        return pageVO;
    }

    @Override
    public List<Long> getTidsByTgids(List<Long> tgids) {
        return terminalInfoMapper.selectList(new LambdaQueryWrapper<TerminalInfoDO>()
                .select(TerminalInfoDO::getTid)
                .in(TerminalInfoDO::getTgid, tgids))
                .stream().map(TerminalInfoDO::getTid).toList();
    }
}
