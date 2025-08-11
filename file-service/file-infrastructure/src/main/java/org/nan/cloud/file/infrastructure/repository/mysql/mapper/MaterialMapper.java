package org.nan.cloud.file.infrastructure.repository.mysql.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MaterialMapper {
    @Select("SELECT file_id FROM material WHERE mid = #{mid} LIMIT 1")
    String selectFileIdByMaterialId(@Param("mid") Long materialId);
}

