
<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

# 8 数据库连接 JDBC
&emsp;&emsp;JDBC(java Data Base Connectivity, Java 数据库连接)是一种用于执行SQL语句的 Java API，可以为多种数据库提供统一访问，它由一组用Java语言编写的类和接口组成。JDBC 为数据库开发人员提供了一个标准的API，据此可以构建更高级的工具和接口，使数据库开发人员能够用纯 Java API 编写数据库应用程序，并且可以跨平台运行，并不受数据库供应商的限制。
&emsp;&emsp;JDBC连接数据库的流程及其原理就不细述了，大体可以分为如下：
1. 加载指定数据库的驱动程序。(引用指定数据库的程序包)
2. 在Java程序中加载驱动程序。
3. 创建数据库连接。
4. 创建 Statement 对象。
5. 调用 Statement 对象的相关方法执行相对应的SQL语句。通过execuUpdate()方法来对数据更新，包括插入和删除等作用。
6. 关闭数据库连接。使用完数据库或者不需要访问数据库时，通过Connection 的 close() 方法及时关闭数据库连接。

## 8.1 Spring 连接数据库程序实现(JDBC)
&emsp;&emsp;Spring 中的 JDBC 连接与直接使用JDBC去连接还是有所差别的，Spring 对JDBC 做了大量封装，消除了冗余代码，使得开发量大大减少。下面通过一个小例子让他加简单认识 Spring JDBC 操作。
1. 创建表
 ```sql
 CREATE TABLE `user` (
    `id` int(11) NOT NULL auto_increment,
    `name` varchar(255) default NULL,
    `age` int(11) default NULL,
    `sex` varchar(10) default NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
 ```
2. 创建对应的数据库PO
 ```java
 public class User {
    private int id;
    private String name;
    private int age;
    private String sex;
    //省略 getter/setter constructor
 }
 ```
3. 创建表与实体间的映射
 ```java
public class UserRowMapper implements RowMapper {
    @Override
    public Object mapRow(ResultSet set, int i) throws SQLException {
        User person = new User(set.getInt("id"),set.getString("name"),set.getInt("age"),set.getString("sex"));
        return person;
    }
}
 ```
4. 创建数据操作接口和实现类
 ```java
 public interface UserService {
    void save(User user);
    List<User> getUsers();
}
public class UserServiceImpl implements UserService{

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
}
 ```
5. 配置文件
 ```xml
    <bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource" destroy-method="close">
        <property name="driverClassName" value="com.mysql.jdbc.Driver"/>
        <property name="jdbcUrl" value="jdbc:mysql://localhost:3306/forum?useUnicode=true&amp;characterEncoding=utf-8"/>
        <property name="username" value="root"/>
        <property name="password" value="12345678"/>
    </bean>

    <bean id="userService" class="info.tonylee.studio.spring.jdbc.UserServiceImpl">
        <property name="dataSource" ref="dataSource"/>
    </bean>
 ```
6.测试
 ```java
    public static void main(String[] args) {
        ApplicationContext act = new ClassPathXmlApplicationContext("/META-INF/jdbc/datasource.xml");
        UserService userService = (UserService)act.getBean("userService");
        User user = new User();
        user.setName("张三");
        user.setAge(20);
        user.setSex("男");
        userService.save(user);
        List<User> list = userService.getUsers();
        for(User person : list){
            System.out.println(person);
        }
    }
 ```

## 8.2 save/update 功能的实现
&emsp;&emsp;通过上面的例子为基础我们开始分析 Spring 中对 JDBC 的支持，首先寻找整个功能的切入点。
&emsp;&emsp;在 UserServiceImpl 中 jdbcTemplate 初始化是从 setDataSource 函数开始的，DataSource 示例通过参数注入，DataSource 的创建过程是引入第三方的连接池。DataSource 是整个数据库操作的基础，里面封装了整个数据库的连接信息。我们首先以保存实体类为例进行代码跟踪。
&emsp;&emsp;对于操作中我们只需提供SQL语句及语句中对应的参数和参数类型，其他操作便可以交给 Spring 来完成了，这些工作到底包括什么呢？进入 JdbcTemplate 中的 update 方法。
 ```java
    public int update(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        return this.update(sql, this.newArgTypePreparedStatementSetter(args, argTypes));
    }

	public int update(String sql, @Nullable PreparedStatementSetter pss) throws DataAccessException {
		return update(new SimplePreparedStatementCreator(sql), pss);
	}
 ```
&emsp;&emsp;进入 update 方法后， Spring 并不急于进入核心处理逻辑，而是先做准备工作，使用 ArgTypePreparedStatementStter 对参数和参数类型进行封装，同时又使用 Simple PreparedStatementCreator 对 SQL 语句进行封装。至于为什么这么封装，暂且留下悬念。
&emsp;&emsp;经过数据库封装后便可以进入核心的数据处理代码了。
 ```java
    protected int update(final PreparedStatementCreator psc, @Nullable final PreparedStatementSetter pss)
			throws DataAccessException {

		logger.debug("Executing prepared SQL update");

		return updateCount(execute(psc, ps -> {
			try {
				if (pss != null) {
					//设置 PreparedStatement 所需的所有参数
					pss.setValues(ps);
				}
				int rows = ps.executeUpdate();
				if (logger.isTraceEnabled()) {
					logger.trace("SQL update affected " + rows + " rows");
				}
				return rows;
			}
			finally {
				if (pss instanceof ParameterDisposer) {
					((ParameterDisposer) pss).cleanupParameters();
				}
			}
		}));
	}
 ```
&emsp;&emsp;如果读者了解过其他操作方法，可以知道 execute 方法是最基础的操作，而其他操作比如 update、query 等方法则是传入不同的 PreparedStatementCallback 参数来执行不同的逻辑。

### 8.2.1 基础方法 execute
&emsp;&emsp;execute 作为数据库操作的核心入口，将大多数数据库操作相同的步骤统一封装，而将个性化的操作使用参数 PreparedStatementCallback 进行回调。
 ```java
    @Override
	@Nullable
	public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action)
			throws DataAccessException {

		Assert.notNull(psc, "PreparedStatementCreator must not be null");
		Assert.notNull(action, "Callback object must not be null");
		if (logger.isDebugEnabled()) {
			String sql = getSql(psc);
			logger.debug("Executing prepared SQL statement" + (sql != null ? " [" + sql + "]" : ""));
		}

		//获取数据库连接
		Connection con = DataSourceUtils.getConnection(obtainDataSource());
		PreparedStatement ps = null;
		try {
			ps = psc.createPreparedStatement(con);
			//应用用户设置的输入参数
			applyStatementSettings(ps);
			//调用回调函数
			T result = action.doInPreparedStatement(ps);
			handleWarnings(ps);
			return result;
		}
		catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			//释放数据库链接避免当 异常转换器没有被初始化的时候出现潜在的连接池死锁
			if (psc instanceof ParameterDisposer) {
				((ParameterDisposer) psc).cleanupParameters();
			}
			String sql = getSql(psc);
			psc = null;
			JdbcUtils.closeStatement(ps);
			ps = null;
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw translateException("PreparedStatementCallback", sql, ex);
		}
		finally {
			if (psc instanceof ParameterDisposer) {
				((ParameterDisposer) psc).cleanupParameters();
			}
			JdbcUtils.closeStatement(ps);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}
 ```
以上方法对常用操作进行了封装，包括如下几项内容。
1. 获取数据库连接
&emsp;&emsp;获取数据库连接也并非直接使用 dataSource.getConnection()方法那么简单，同样也考虑呢诸多情况。
 ```java
    public static Connection doGetConnection(DataSource dataSource) throws SQLException {
		Assert.notNull(dataSource, "No DataSource specified");

		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
		if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
			conHolder.requested();
			if (!conHolder.hasConnection()) {
				logger.debug("Fetching resumed JDBC Connection from DataSource");
				conHolder.setConnection(fetchConnection(dataSource));
			}
			return conHolder.getConnection();
		}
		// Else we either got no holder or an empty thread-bound holder here.

		logger.debug("Fetching JDBC Connection from DataSource");
		Connection con = fetchConnection(dataSource);

		//当前线程支持同步
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			try {
				// Use same Connection for further JDBC actions within the transaction.
				// Thread-bound object will get removed by synchronization at transaction completion.
				//在事务中使用同一数据库连接
				ConnectionHolder holderToUse = conHolder;
				if (holderToUse == null) {
					holderToUse = new ConnectionHolder(con);
				}
				else {
					holderToUse.setConnection(con);
				}
				//记录数据库连接
				holderToUse.requested();
				TransactionSynchronizationManager.registerSynchronization(
						new ConnectionSynchronization(holderToUse, dataSource));
				holderToUse.setSynchronizedWithTransaction(true);
				if (holderToUse != conHolder) {
					TransactionSynchronizationManager.bindResource(dataSource, holderToUse);
				}
			}
			catch (RuntimeException ex) {
				// Unexpected exception from external delegation call -> close Connection and rethrow.
				releaseConnection(con, dataSource);
				throw ex;
			}
		}

		return con;
	}
 ```
&emsp;&emsp;在数据库连接方法面，Spring 主要考虑的是关于事务方面的处理。基于事务处理的特殊性，Spring 需要保证线程中的数据库操作都是使用同一事务连接。

2. 应用用户设定的输入参数
 ```java
    protected void applyStatementSettings(Statement stmt) throws SQLException {
		int fetchSize = getFetchSize();
		if (fetchSize != -1) {
			stmt.setFetchSize(fetchSize);
		}
		int maxRows = getMaxRows();
		if (maxRows != -1) {
			stmt.setMaxRows(maxRows);
		}
		DataSourceUtils.applyTimeout(stmt, getDataSource(), getQueryTimeout());
	}
 ```
&emsp;&emsp;setFetchSize 最主要是为了减少网络交互次数设计的。访问 ResultSet 时，如果它每次只从服务器上读取一行数据，则会产生大量的开销。setFetchSize 的意思是当调用 rs.next 时， ResultSet 会一次性从服务器上取得多少行数据回来，这样在下次 rs.next 时，它可以直接从内存中获取数据而不需要网络交互，提高了效率。这个设置可能被某些JDBC驱动忽略，而设置过大也会造成内存的上升。
&emsp;&emsp;setMaxRows 将池 Statement 对象生成的所有 ResultSet 对象可以包含的最大行数限制为给定数。

3. 调用回调函数
&emsp;&emsp;处理一些通用方法外的个性化处理，也就是 PreparedStatementCallback 类型的参数的 doInPreparedStatement 方法的回调。

4. 警告处理
 ```java
    protected void handleWarnings(Statement stmt) throws SQLException {
		//当前设置为忽略警告时只尝试打印日志
		if (isIgnoreWarnings()) {
			if (logger.isDebugEnabled()) {
				//如果日志开启的情况就是打印日志
				SQLWarning warningToLog = stmt.getWarnings();
				while (warningToLog != null) {
					logger.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() + "', error code '" +
							warningToLog.getErrorCode() + "', message [" + warningToLog.getMessage() + "]");
					warningToLog = warningToLog.getNextWarning();
				}
			}
		}
		else {
			handleWarnings(stmt.getWarnings());
		}
	}
 ```
&emsp;&emsp;这里用到了一个类 SQLWarning, SQLWarning 提供关于数据库访问警告信息的异常。这些警告直接链接到导致报告警告的方法所在的对象。警告可以从 Connection、Statement 和 ResultSet 对象中后去。试图在已经关闭的连接上获取警告将导致抛出异常。类似地，试图在已经关闭的语句上或已经关闭的结果集上获取警告也将导致抛出异常。注意，关闭语句时还会关闭它可能生成的结果集。
&emsp;&emsp;很多人不是很理解什么情况下回产生警告而不是异常，在这里给读者提示一个最常见的警告 DataTruncation:DataTruncation 直接继承 SQLWarning，由于某种原因以外的截取数据值时会以 DataTruncation 警告形式报告异常。
&emsp;&emsp;对于警告的处理方式并不是抛出异常，出现警告很可能会出现数据错误，但是，并不一定会影响程序执行，所以用户可以自己设置处理警告的方法，如默认的是忽略警告，当出现警告时只打印警告日志，而另一种方式只直接抛出异常。

5. 资源释放
&emsp;&emsp;数据库的链接释放并不是直接调用 Connection 的 API 中的 close 方法。考虑到存在事务的情况，如果当前线程存事务，那么说明在当前线程中存在公用数据库连接，这种情况下直接使用 ConnectionHolder 中的 released 方法进行连接数减一，而不是真正的释放连接。
 ```java
    public static void releaseConnection(@Nullable Connection con, @Nullable DataSource dataSource) {
		try {
			doReleaseConnection(con, dataSource);
		}
		catch (SQLException ex) {
			logger.debug("Could not close JDBC Connection", ex);
		}
		catch (Throwable ex) {
			logger.debug("Unexpected exception on closing JDBC Connection", ex);
		}
	}

    public static void doReleaseConnection(@Nullable Connection con, @Nullable DataSource dataSource) throws SQLException {
		if (con == null) {
			return;
		}
		if (dataSource != null) {
			//当线程存在的事务的情况下说明存在共用数据库连接直接使用 ConnectionHolder 中的 released 方法进行连接数减一而不是真正的释放连接
			ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
			if (conHolder != null && connectionEquals(conHolder, con)) {
				// It's the transactional Connection: Don't close it.
				conHolder.released();
				return;
			}
		}
		doCloseConnection(con, dataSource);
	}

    public static void doCloseConnection(Connection con, @Nullable DataSource dataSource) throws SQLException {
		if (!(dataSource instanceof SmartDataSource) || ((SmartDataSource) dataSource).shouldClose(con)) {
			con.close();
		}
	}
 ```
### 8.2.2 Update 中的回调函数
&emsp;&emsp;PreparedStatementCallback 作为一个接口，其中只有一个函数 doInPreparedStatement，这个函数是用于调用通用方法 execute 的时候无法处理的一些个性化处理方法，在 update 中的函数实现：
&emsp;&emsp;其中真正执行SQL 的 ps.executeUpdate 没有太多需要讲解的，因为我们平时在直接使用JDBC方式进行调用的事发后会经常使用此方法。但是，对于设置输入参数的函数 pass.setValues(ps)，我们有必要去深入研究一下。在没有分析源码之前，我们至少可以知道其功能，不妨回顾下 Spring 中使用 SQL 的执行过程，直接如下：
 ```java
 jdbcTemplate.update("insert into user(`name`,age,sex) values (?,?,?)",
                new Object[]{user.getName() ,user.getAge() , user.getSex()},
                new int[]{Types.VARCHAR,Types.INTEGER, Types.VARCHAR});
 ```
&emsp;&emsp;SQL语句对应的参数，对应参数的类型清晰明了，这都归功于Spring为我们做了封装，而真正的JDBC调用其实非常繁琐，你需要这么做：
 ```java
 PreparedStatement updateSales = con.prepareStatement("nsert into user(`name`,age,sex) values (?,?,?)");
 updateSales.setString(1, user.getName());
 updateSales.setInt(2, user.getAge());
 updateSales.setString(3, user.getSex());
 ```
&emsp;&emsp;那么我们看看 Spring 是如何做到封装上面的操作呢？
&emsp;&emsp;首先，所有的操作都是以pss.setValues(ps)为入口。这个pass所代表的的当前类正式 ArsPreparedStatementStter。 其中 setValues 如下：
 ```java
 	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		if (this.args != null) {
			for (int i = 0; i < this.args.length; i++) {
				Object arg = this.args[i];
				//解析当前属性
				doSetValue(ps, i + 1, arg);
			}
		}
	}
	protected void doSetValue(PreparedStatement ps, int parameterPosition, Object argValue) throws SQLException {
		if (argValue instanceof SqlParameterValue) {
			SqlParameterValue paramValue = (SqlParameterValue) argValue;
			StatementCreatorUtils.setParameterValue(ps, parameterPosition, paramValue, paramValue.getValue());
		}
		else {
			StatementCreatorUtils.setParameterValue(ps, parameterPosition, SqlTypeValue.TYPE_UNKNOWN, argValue);
		}
	}
	
 ```
>StatementCreatorUtils
 ```java
 	public static void setParameterValue(PreparedStatement ps, int paramIndex, int sqlType,
			@Nullable Object inValue) throws SQLException {

		setParameterValueInternal(ps, paramIndex, sqlType, null, null, inValue);
	}

	private static void setParameterValueInternal(PreparedStatement ps, int paramIndex, int sqlType,
			@Nullable String typeName, @Nullable Integer scale, @Nullable Object inValue) throws SQLException {

		String typeNameToUse = typeName;
		int sqlTypeToUse = sqlType;
		Object inValueToUse = inValue;

		// override type info?
		if (inValue instanceof SqlParameterValue) {
			SqlParameterValue parameterValue = (SqlParameterValue) inValue;
			if (logger.isDebugEnabled()) {
				logger.debug("Overriding type info with runtime info from SqlParameterValue: column index " + paramIndex +
						", SQL type " + parameterValue.getSqlType() + ", type name " + parameterValue.getTypeName());
			}
			if (parameterValue.getSqlType() != SqlTypeValue.TYPE_UNKNOWN) {
				sqlTypeToUse = parameterValue.getSqlType();
			}
			if (parameterValue.getTypeName() != null) {
				typeNameToUse = parameterValue.getTypeName();
			}
			inValueToUse = parameterValue.getValue();
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Setting SQL statement parameter value: column index " + paramIndex +
					", parameter value [" + inValueToUse +
					"], value class [" + (inValueToUse != null ? inValueToUse.getClass().getName() : "null") +
					"], SQL type " + (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN ? "unknown" : Integer.toString(sqlTypeToUse)));
		}

		if (inValueToUse == null) {
			setNull(ps, paramIndex, sqlTypeToUse, typeNameToUse);
		}
		else {
			setValue(ps, paramIndex, sqlTypeToUse, typeNameToUse, scale, inValueToUse);
		}
	}
 ```

## 8.3 query 功能的实现
&emsp;&emsp;上面我们讲解了 update 方法的功能实现，那么在数据库操作中查找操作也是使用率非常高的函数，那么我们也需要了解它的实现过程。使用方法如下：
 ```java
 List<User> list = jdbcTemplate.query("select * from user where id = ?", new Object[]{20}, new int[]{java.sql.Types.INTEGER}, new UserRowMapper());
 ```
&emsp;&emsp;跟踪 jdbcTemplate 中的 query 方法。
 ```java
 	@Override
	public <T> List<T> query(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException {
		return result(query(sql, args, argTypes, new RowMapperResultSetExtractor<>(rowMapper)));
	}
	
	@Override
	@Nullable
	public <T> T query(String sql, Object[] args, int[] argTypes, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(sql, newArgTypePreparedStatementSetter(args, argTypes), rse);
	}

	@Override
	@Nullable
	public <T> T query(String sql, @Nullable Object[] args, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(sql, newArgPreparedStatementSetter(args), rse);
	}

	@Override
	@Nullable
	public <T> T query(String sql, @Nullable PreparedStatementSetter pss, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(new SimplePreparedStatementCreator(sql), pss, rse);
	}

	public <T> T query(
			PreparedStatementCreator psc, @Nullable final PreparedStatementSetter pss, final ResultSetExtractor<T> rse)
			throws DataAccessException {

		Assert.notNull(rse, "ResultSetExtractor must not be null");
		logger.debug("Executing prepared SQL query");

		return execute(psc, new PreparedStatementCallback<T>() {
			@Override
			@Nullable
			public T doInPreparedStatement(PreparedStatement ps) throws SQLException {
				ResultSet rs = null;
				try {
					if (pss != null) {
						pss.setValues(ps);
					}
					rs = ps.executeQuery();
					return rse.extractData(rs);
				}
				finally {
					JdbcUtils.closeResultSet(rs);
					if (pss instanceof ParameterDisposer) {
						((ParameterDisposer) pss).cleanupParameters();
					}
				}
			}
		});
	}
 ```
&emsp;&emsp;其实可以看到整体的套路和update差不多，只不过在回调类 PreparedStatementCallback 的实现中使用的是 ps.executeQuery()执行查询操作，而且在返回方法上也做了一些额外的处理。
&emsp;&emsp;rse.extractData(rsToUse)方法负责将结果进行封装并装换至POJO，rse 当前代表的类为 RowMapperResultSetExtractor ，而在构造函数 RowMapperResultSetExtractor 的时候我们又将自定义的 rowMapper 设置了进去。调用代码如下：
>RowMapperResultSetExtractor
 ```java
 	@Override
	public List<T> extractData(ResultSet rs) throws SQLException {
		List<T> results = (this.rowsExpected > 0 ? new ArrayList<>(this.rowsExpected) : new ArrayList<>());
		int rowNum = 0;
		while (rs.next()) {
			results.add(this.rowMapper.mapRow(rs, rowNum++));
		}
		return results;
	}
 ```
&emsp;&emsp;上面的代码中并没有什么复杂的逻辑，只是对返回结果遍历并以此使用rowMapper进行转换。
&emsp;&emsp;之前讲了 update 方法以及 query 方法，这两个函数示例的 SQL 都是带有参数的，也就是带有 ? 的，那么还有另一种情况不带 ? 的，Spring 使用的是另一种处理方式。
例如：
 ```java
  	List<User> list = jdbcTemplate.query("select * from user", new UserRowMapper());
 ```
>JdbcTemplate
 ```java
 	@Override
	public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		return result(query(sql, new RowMapperResultSetExtractor<>(rowMapper)));
	}
	@Override
	@Nullable
	public <T> T query(final String sql, final ResultSetExtractor<T> rse) throws DataAccessException {
		Assert.notNull(sql, "SQL must not be null");
		Assert.notNull(rse, "ResultSetExtractor must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL query [" + sql + "]");
		}

		/**
		 * Callback to execute the query.
		 */
		class QueryStatementCallback implements StatementCallback<T>, SqlProvider {
			@Override
			@Nullable
			public T doInStatement(Statement stmt) throws SQLException {
				ResultSet rs = null;
				try {
					rs = stmt.executeQuery(sql);
					return rse.extractData(rs);
				}
				finally {
					JdbcUtils.closeResultSet(rs);
				}
			}
			@Override
			public String getSql() {
				return sql;
			}
		}

		return execute(new QueryStatementCallback());
	}
 ```
&emsp;&emsp;与之前的 query 方法最大的不同是少了参数及参数类型的传递，自然也少了 PreparedStatementSetter 类型的封装。既然少了 PreparedStatementStter 类型的传入，调用的 execute 方法自然也会有所改变了。
 ```java
 @Override
	@Nullable
	public <T> T execute(StatementCallback<T> action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");

		Connection con = DataSourceUtils.getConnection(obtainDataSource());
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			applyStatementSettings(stmt);
			T result = action.doInStatement(stmt);
			handleWarnings(stmt);
			return result;
		}
		catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			String sql = getSql(action);
			JdbcUtils.closeStatement(stmt);
			stmt = null;
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw translateException("StatementCallback", sql, ex);
		}
		finally {
			JdbcUtils.closeStatement(stmt);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}
 ```
&emsp;&emsp;这个 execute 与之前的 execute 并无太大差别，都是做一些常规的处理，诸如获取连接、释放连接等，但是，有一个地方是不一样的，就是 statement 的创建。这里直接使用 connection 创建，而且带有参数的 SQL 使用的是 PreparedStatementCreator 类来创建的。一个是普通的 Statement ，另一个是 PreparedStatement， 两者究竟是什么区别呢？
&emsp;&emsp;PreparedStatement 接口继承 Statement ， 并与之在量方面有所不同。
* PreparedStatement 实例包含已编译的 SQL 语句。这就是使语句 “准备好”。包含于 PreparedStatement 对象中的 SQL 语句可具有一个或多个IN参数。IN参数的值在SQL语句创建时未被指定。相反的，该语句为每个IN参数保留一个问号("?")作为占位符。每个问号的值必须在该语句执行之前，通过适当的setXXX方法来提供。
* 由于 PreparedStatement 对象已预编译过，所以其执行速度要快于 Statement 对象。因此，多次执行的SQL语句经常创建为 PreparedStatement 对象，以提高效率。
&emsp;&emsp;作为 Statement 的子类，PreparedStatement 继承了 Statement 的所有功能。另外它还添加了一整套方法，用于设置发给数据库以取代 IN 参数占位符的值。同时，三种方法 execute、executeQuery和executeUpdate已被更改以使之不再需要参数。这些方法的 Statement 形式(接受SQL语句参数的形式)不应该用于 PreparedStatement 对象。

## 8.4 queryForObject
&emsp;&emsp;Spring中不仅仅为我们提供了query方法，还在此基础上做了封装，提供了不同类型的query方法。
&emsp;&emsp;我们以queryForObject为例，来讨论一下Spring是如何在返回结果的基础上进行封装的。
 ```java
 	@Override
	@Nullable
	public <T> T queryForObject(String sql, Class<T> requiredType) throws DataAccessException {
		return queryForObject(sql, getSingleColumnRowMapper(requiredType));
	}

	public T mapRow(ResultSet rs, int rowNum) throws SQLException {
		// Validate column count.
		// 验证返回的结果数
		ResultSetMetaData rsmd = rs.getMetaData();
		int nrOfColumns = rsmd.getColumnCount();
		if (nrOfColumns != 1) {
			throw new IncorrectResultSetColumnCountException(1, nrOfColumns);
		}

		// Extract column value from JDBC ResultSet.
		Object result = getColumnValue(rs, 1, this.requiredType);
		if (result != null && this.requiredType != null && !this.requiredType.isInstance(result)) {
			// Extracted value does not match already: try to convert it.
			try {
				return (T) convertValueToRequiredType(result, this.requiredType);
			}
			catch (IllegalArgumentException ex) {
				throw new TypeMismatchDataAccessException(
						"Type mismatch affecting row number " + rowNum + " and column type '" +
						rsmd.getColumnTypeName(1) + "': " + ex.getMessage());
			}
		}
		return (T) result;
	}
 ```