package info.tonylee.studio.spring.mybatis;

import info.tonylee.studio.spring.jdbc.User;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

public class TestMapper{
    static SqlSessionFactory sqlSessionFactory = null;
    static{
        sqlSessionFactory = MybatisUtil.getSqlSessionFactory();
    }

    public void testAdd(){
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try{
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            User user = new User("tony",new Integer(10));
            userMapper.insertUser(user);
            sqlSession.commit();
        }finally {
            sqlSession.close();
        }
    }

    public void getUser(){
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try{
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            System.out.println(userMapper.getUser(1));
        }finally {
            sqlSession.close();
        }
    }

    public static void main(String[] args) {
        TestMapper testMapper = new TestMapper();
        testMapper.testAdd();
        testMapper.getUser();
    }
}
