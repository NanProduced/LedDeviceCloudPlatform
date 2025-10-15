package org.nan.cloud.core.service.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.domain.ProgramApproval;
import org.nan.cloud.program.dto.response.ProgramApprovalDTO;

import java.util.List;

/**
 * 节目审核DTO转换器
 * 专门处理ProgramApproval Domain对象与DTO对象之间的转换
 * 符合DDD Application层职责：处理业务逻辑和对外接口数据转换
 */
@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ProgramApprovalDtoConverter {

    /**
     * Domain对象转换为ProgramApprovalDTO
     * @param approval Domain对象
     * @return ProgramApprovalDTO
     */
    @Mapping(source = "oid", target = "oid")
    ProgramApprovalDTO toProgramApprovalDTO(ProgramApproval approval);
    
    /**
     * 批量Domain对象转换为ProgramApprovalDTO
     * @param approvals Domain对象列表
     * @return ProgramApprovalDTO列表
     */
    List<ProgramApprovalDTO> toProgramApprovalDTOs(List<ProgramApproval> approvals);
    
    /**
     * DTO转换为Domain对象（用于服务层接收请求时的转换）
     * @param approvalDTO DTO对象
     * @return Domain对象
     */
    @Mapping(source = "oid", target = "oid")
    ProgramApproval toDomain(ProgramApprovalDTO approvalDTO);
}