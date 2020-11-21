package info.tonylee.studio.spring.jdbc;

import java.util.List;

public interface UserService {

    void save(User user);

    List<User> getUsers();

    User getUser();

}
