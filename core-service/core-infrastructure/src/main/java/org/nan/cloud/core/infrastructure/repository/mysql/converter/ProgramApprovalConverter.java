package org.nan.cloud.core.infrastructure.repository.mysql.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.domain.ProgramApproval;
import org.nan.cloud.program.entity.ProgramApprovalDO;

import java.util.List;

/**
 * 节目审核记录领域对象转换器
 * 专门处理ProgramApproval Domain对象与ProgramApprovalDO对象之间的转换
 * 符合DDD Infrastructure层职责：仅处理数据持久化相关转换
 */
@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ProgramApprovalConverter {

    /**
     * DO转换为Domain对象
     * @param approvalDO 数据对象
     * @return Domain对象
     */
    @Mapping(source = "orgId", target = "oid")
    ProgramApproval toDomain(ProgramApprovalDO approvalDO);
    
    /**
     * Domain对象转换为DO
     * @param approval Domain对象
     * @return 数据对象
     */
    @Mapping(source = "oid", target = "orgId")
    ProgramApprovalDO toDO(ProgramApproval approval);
    
    /**
     * 批量DO转换为Domain对象
     * @param approvalDOs 数据对象列表
     * @return Domain对象列表
     */
    List<ProgramApproval> toDomains(List<ProgramApprovalDO> approvalDOs);
    
    /**
     * 批量Domain对象转换为DO
     * @param approvals Domain对象列表
     * @return 数据对象列表
     */
    List<ProgramApprovalDO> toDOs(List<ProgramApproval> approvals);
}