package info.tonylee.studio.spring.jdbc;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;

public class UserServiceImpl implements UserService {

    private JdbcTemplate jdbcTemplate;

    public void setDataSource(DataSource dataSource){
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void save(User user) {
        jdbcTemplate.update("insert into user(`name`,age,sex) values (?,?,?)",
                new Object[]{user.getName() ,user.getAge() , user.getSex()},
                new int[]{Types.VARCHAR,Types.INTEGER, Types.VARCHAR});
    }

    @Override
    public List<User> getUsers() {
        List<User> list = jdbcTemplate.query("select * from user", new UserRowMapper());
        return list;
    }

    @Override
    public User getUser(){
        return jdbcTemplate.queryForObject("select * from user where id = ?",new Object[]{1}, User.class);
    }

    public static void main(String[] args) {
        ApplicationContext act = new ClassPathXmlApplicationContext("/META-INF/jdbc/datasource.xml");
        UserService userService = (UserService)act.getBean("userService");

        User user = new User();
        user.setName("张三");
        user.setAge(20);
        user.setSex("男");
        userService.save(user);
        System.out.println(userService.getUser());
        List<User> list = userService.getUsers();
        for(User person : list){
            System.out.println(person);
        }
    }
}
