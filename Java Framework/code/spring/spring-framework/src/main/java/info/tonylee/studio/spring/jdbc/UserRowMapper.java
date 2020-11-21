package info.tonylee.studio.spring.jdbc;


import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper {
    @Override
    public Object mapRow(ResultSet set, int i) throws SQLException {
        User person = new User(set.getInt("id"),set.getString("name"),set.getInt("age"),set.getString("sex"));
        return person;
    }
}
