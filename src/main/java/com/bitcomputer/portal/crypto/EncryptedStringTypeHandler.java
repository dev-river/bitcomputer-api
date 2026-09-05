package com.bitcomputer.portal.crypto;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    @Autowired
    private AesCryptoUtil aesCryptoUtil;

    public EncryptedStringTypeHandler() {
    }

    public EncryptedStringTypeHandler(AesCryptoUtil aesCryptoUtil) {
        this.aesCryptoUtil = aesCryptoUtil;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, aesCryptoUtil.encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return aesCryptoUtil.decrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return aesCryptoUtil.decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return aesCryptoUtil.decrypt(cs.getString(columnIndex));
    }
}
