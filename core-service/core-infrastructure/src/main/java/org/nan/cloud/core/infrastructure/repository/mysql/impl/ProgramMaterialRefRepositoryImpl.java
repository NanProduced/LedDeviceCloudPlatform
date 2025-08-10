package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.domain.ProgramMaterialRef;
import org.nan.cloud.core.repository.ProgramMaterialRefRepository;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.ProgramMaterialRefMapper;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.ProgramMaterialRefConverter;
import org.nan.cloud.program.entity.ProgramMaterialRefDO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 节目素材引用Repository实现
 * 符合DDD Infrastructure层职责：处理数据持久化和Domain对象转换
 * 
 * TODO: 完善实现，当前为简化版本以解决编译问题
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProgramMaterialRefRepositoryImpl implements ProgramMaterialRefRepository {

    private final ProgramMaterialRefMapper programMaterialRefMapper;
    private final org.nan.cloud.core.infrastructure.repository.mysql.converter.ProgramMaterialRefConverter converter;

    @Override
    public List<ProgramMaterialRef> findByProgramId(Long programId) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProgramMaterialRefDO>()
                .eq(ProgramMaterialRefDO::getProgramId, programId)
                .orderByAsc(ProgramMaterialRefDO::getUsageIndex);
        List<ProgramMaterialRefDO> list = programMaterialRefMapper.selectList(wrapper);
        return converter.toDomains(list);
    }

    @Override
    public List<ProgramMaterialRef> findByProgramIdAndVersion(Long programId, Integer version) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProgramMaterialRefDO>()
                .eq(ProgramMaterialRefDO::getProgramId, programId)
                .eq(ProgramMaterialRefDO::getProgramVersion, version)
                .orderByAsc(ProgramMaterialRefDO::getUsageIndex);
        List<ProgramMaterialRefDO> list = programMaterialRefMapper.selectList(wrapper);
        return converter.toDomains(list);
    }

    @Override
    public List<ProgramMaterialRef> findByMaterialId(Long materialId) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProgramMaterialRefDO>()
                .eq(ProgramMaterialRefDO::getMaterialId, materialId);
        List<ProgramMaterialRefDO> list = programMaterialRefMapper.selectList(wrapper);
        return converter.toDomains(list);
    }

    @Override
    public boolean isMaterialUsed(Long materialId) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProgramMaterialRefDO>()
                .eq(ProgramMaterialRefDO::getMaterialId, materialId)
                .last("limit 1");
        Long count = programMaterialRefMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public int saveBatch(List<ProgramMaterialRef> refs) {
        if (refs == null || refs.isEmpty()) return 0;
        List<ProgramMaterialRefDO> rows = converter.toDOs(refs);
        int affected = 0;
        for (ProgramMaterialRefDO row : rows) {
            affected += programMaterialRefMapper.insert(row);
        }
        return affected;
    }

    @Override
    public int deleteByProgramId(Long programId) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProgramMaterialRefDO>()
                .eq(ProgramMaterialRefDO::getProgramId, programId);
        return programMaterialRefMapper.delete(wrapper);
    }

    @Override
    public int deleteByProgramIdAndVersion(Long programId, Integer version) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProgramMaterialRefDO>()
                .eq(ProgramMaterialRefDO::getProgramId, programId)
                .eq(ProgramMaterialRefDO::getProgramVersion, version);
        return programMaterialRefMapper.delete(wrapper);
    }

    @Override
    public long countUsageByMaterialId(Long materialId) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProgramMaterialRefDO>()
                .eq(ProgramMaterialRefDO::getMaterialId, materialId);
        Long cnt = programMaterialRefMapper.selectCount(wrapper);
        return cnt == null ? 0L : cnt;
    }

    @Override
    public Map<String, Integer> getMaterialTypeStatistics(Long programId, Integer version) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProgramMaterialRefDO>()
                .eq(ProgramMaterialRefDO::getProgramId, programId)
                .eq(ProgramMaterialRefDO::getProgramVersion, version);
        List<ProgramMaterialRefDO> list = programMaterialRefMapper.selectList(wrapper);
        return list.stream().collect(java.util.stream.Collectors.toMap(
                ProgramMaterialRefDO::getMaterialType,
                x -> 1,
                Integer::sum
        ));
    }
}