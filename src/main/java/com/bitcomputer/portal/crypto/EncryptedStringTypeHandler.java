package com.bitcomputer.portal.crypto;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class EncryptedStringTypeHandler implements TypeHandler<String> {

    private final AesCryptoUtil aesCryptoUtil;

    public EncryptedStringTypeHandler(AesCryptoUtil aesCryptoUtil) {
        this.aesCryptoUtil = aesCryptoUtil;
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter == null ? null : aesCryptoUtil.encrypt(parameter));
    }

    @Override
    public String getResult(ResultSet rs, String columnName) throws SQLException {
        String raw = rs.getString(columnName);
        return raw == null ? null : aesCryptoUtil.decrypt(raw);
    }

    @Override
    public String getResult(ResultSet rs, int columnIndex) throws SQLException {
        String raw = rs.getString(columnIndex);
        return raw == null ? null : aesCryptoUtil.decrypt(raw);
    }

    @Override
    public String getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String raw = cs.getString(columnIndex);
        return raw == null ? null : aesCryptoUtil.decrypt(raw);
    }
}
