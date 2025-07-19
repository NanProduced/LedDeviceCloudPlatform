package org.nan.cloud.core.infrastructure.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.nan.cloud.common.basic.model.BindingType;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * BindingType枚举的MyBatis类型处理器
 * 
 * @author nan
 */
@Component
@MappedTypes(BindingType.class)
public class BindingTypeHandler extends BaseTypeHandler<BindingType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, BindingType parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public BindingType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        if (value == null) {
            return null;
        }
        try {
            return BindingType.valueOf(value);
        } catch (IllegalArgumentException e) {
            // 如果遇到无效的枚举值，返回默认值
            return BindingType.INCLUDE;
        }
    }

    @Override
    public BindingType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        if (value == null) {
            return null;
        }
        try {
            return BindingType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return BindingType.INCLUDE;
        }
    }

    @Override
    public BindingType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        if (value == null) {
            return null;
        }
        try {
            return BindingType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return BindingType.INCLUDE;
        }
    }
}