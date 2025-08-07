package org.nan.cloud.core.infrastructure.repository.mysql.converter;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.domain.Program;
import org.nan.cloud.program.entity.ProgramDO;

import java.util.List;

/**
 * 节目领域对象转换器
 * 专门处理Domain对象与DO对象之间的转换
 * 符合DDD Infrastructure层职责：仅处理数据持久化相关转换
 */
@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ProgramDomainConverter {

    /**
     * DO转换为Domain对象
     * @param programDO 数据对象
     * @return Domain对象
     */
    Program toDomain(ProgramDO programDO);
    
    /**
     * Domain对象转换为DO
     * @param program Domain对象
     * @return 数据对象
     */
    ProgramDO toDO(Program program);
    
    /**
     * 批量DO转换为Domain对象
     * @param programDOs 数据对象列表
     * @return Domain对象列表
     */
    List<Program> toDomains(List<ProgramDO> programDOs);
    
    /**
     * 批量Domain对象转换为DO
     * @param programs Domain对象列表
     * @return 数据对象列表
     */
    List<ProgramDO> toDOs(List<Program> programs);
}