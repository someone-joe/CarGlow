package com.ruoyi.wash.common.mybatis;

import com.alibaba.fastjson2.JSON;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 把 {@code List<String>} 与数据库里的 JSON 文本互转（如服务详情的 items / 样图）。
 * DB 列一律 TEXT 存 JSON 数组；读出来统一是非 null 的空列表，避免前端收到 null。
 */
public class JsonStringListTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, JSON.toJSONString(parameter));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<String> parse(String s) {
        if (s == null || s.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return JSON.parseArray(s, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
