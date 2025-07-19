package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.DTO.SearchTerminalGroupDTO;
import org.nan.cloud.core.DTO.TerminalGroupListDTO;
import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TerminalGroupDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.TerminalGroupMapper;
import org.nan.cloud.core.repository.TerminalGroupRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TerminalGroupRepositoryImpl implements TerminalGroupRepository {

    private final TerminalGroupMapper terminalGroupMapper;

    private final CommonConverter commonConverter;

    @Override
    public TerminalGroup createTerminalGroup(TerminalGroup terminalGroup) {
        TerminalGroupDO terminalGroupDO = commonConverter.terminalGroup2TerminalGroupDO(terminalGroup);
        terminalGroupDO.setCreateTime(LocalDateTime.now());
        terminalGroupMapper.insert(terminalGroupDO);
        String path = terminalGroupDO.getPath().isBlank() ? String.valueOf(terminalGroupDO.getTgid()) : terminalGroupDO.getPath() + "|" + terminalGroupDO.getTgid();
        terminalGroupDO.setPath(path);
        terminalGroupMapper.updateById(terminalGroupDO);
        return commonConverter.terminalGroupDO2TerminalGroup(terminalGroupDO);
    }

    @Override
    public TerminalGroup getTerminalGroupById(Long tgid) {
        return null;
    }

    @Override
    public void updateTerminalGroup(TerminalGroup terminalGroup) {
        terminalGroup.setUpdateTime(LocalDateTime.now());
        terminalGroupMapper.updateById(commonConverter.terminalGroup2TerminalGroupDO(terminalGroup));
    }

    @Override
    public void deleteTerminalGroup(Long tgid) {

    }


    @Override
    public boolean ifTheSameOrg(Long oid, Long tgid) {
        return terminalGroupMapper.exists(new LambdaQueryWrapper<TerminalGroupDO>()
                .eq(TerminalGroupDO::getOid, oid)
                .eq(TerminalGroupDO::getTgid, tgid));
    }

    @Override
    public boolean ifTheSameOrg(Long oid, List<Long> tgids) {
        return terminalGroupMapper.selectCount(new LambdaQueryWrapper<TerminalGroupDO>()
                .eq(TerminalGroupDO::getOid, oid)
                .in(TerminalGroupDO::getTgid, tgids)) == tgids.size();
    }
    
    @Override
    public PageVO<TerminalGroup> searchAccessibleTerminalGroups(Integer pageNum, Integer pageSize, SearchTerminalGroupDTO searchDTO, List<Long> accessibleTerminalGroupIds) {
        Page<TerminalGroupDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TerminalGroupDO> queryWrapper = new LambdaQueryWrapper<TerminalGroupDO>()
                .eq(TerminalGroupDO::getOid, searchDTO.getOid())
                .in(TerminalGroupDO::getTgid, accessibleTerminalGroupIds)
                .like(TerminalGroupDO::getName, searchDTO.getKeyword());
        queryWrapper.orderByDesc(TerminalGroupDO::getCreateTime);
        IPage pageResult = terminalGroupMapper.selectPage(page, queryWrapper);

        PageVO<TerminalGroup> pageVO = PageVO.<TerminalGroup>builder()
                .pageNum((int) pageResult.getCurrent())
                .pageSize((int) pageResult.getSize())
                .total(pageResult.getTotal())
                .records(commonConverter.terminalGroupDO2TerminalGroup(pageResult.getRecords()))
                .build();
        pageVO.calculate();
        return pageVO;
    }
}
