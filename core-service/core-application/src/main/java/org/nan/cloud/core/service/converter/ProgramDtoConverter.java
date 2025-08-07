package org.nan.cloud.core.service.converter;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.domain.Program;
import org.nan.cloud.program.dto.response.DraftDTO;
import org.nan.cloud.program.dto.response.ProgramDTO;
import org.nan.cloud.program.dto.response.ProgramVersionDTO;

import java.util.List;

/**
 * 节目DTO转换器
 * 专门处理Domain对象与DTO对象之间的转换
 * 符合DDD Application层职责：处理业务逻辑和对外接口数据转换
 */
@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ProgramDtoConverter {

    /**
     * Domain对象转换为ProgramDTO
     * @param program Domain对象
     * @return ProgramDTO
     */
    ProgramDTO toProgramDTO(Program program);
    
    /**
     * 批量Domain对象转换为ProgramDTO
     * @param programs Domain对象列表
     * @return ProgramDTO列表
     */
    List<ProgramDTO> toProgramDTOs(List<Program> programs);

    /**
     * Domain对象转换为版本信息DTO
     * @param program Domain对象
     * @return ProgramVersionDTO
     */
    ProgramVersionDTO toProgramVersionDTO(Program program);
    
    /**
     * 批量Domain对象转换为版本信息DTO
     * @param programs Domain对象列表
     * @return ProgramVersionDTO列表
     */
    List<ProgramVersionDTO> toProgramVersionDTOs(List<Program> programs);

    /**
     * Domain对象转换为草稿DTO（当节目状态为DRAFT时）
     * @param program Domain对象
     * @return DraftDTO
     */
    DraftDTO toDraftDTO(Program program);
    
    /**
     * 批量Domain对象转换为草稿DTO
     * @param programs Domain对象列表
     * @return DraftDTO列表
     */
    List<DraftDTO> toDraftDTOs(List<Program> programs);
}