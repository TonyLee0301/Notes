package info.tonylee.studio.spring.mybatis;

import info.tonylee.studio.spring.jdbc.User;

public interface UserMapper {
    void insertUser(User user);
    User getUser(Integer id);
}

