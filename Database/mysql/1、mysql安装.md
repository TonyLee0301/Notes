#MySql 安装
&ensp;&#8194;下载地址：https://dev.mysql.com/downloads/mysql/
&ensp;&#8194;选择对应的版本下载；
##一、Linux 安装；
###1、安装mysql，初始化表
####1.1.、首先运行以下命令并解压
```shell{.line-numbers}
[root@localhost root]# xz -d mysql-8.0.12-linux-glibc2.12-x86_64.tar.xz
```
会生成一个tar文件，再次进行解压，并拷贝到对应的安装目录下：   
```shell{.line-numbers}
[root@localhost root]# tar -xvf mysql-8.0.12-linux-glibc2.12-x86_64.tar mysql
[root@localhost root]# mv mysql /usr/local/mysql
```
####1.2、添加用户组、用户名：    
```shell{.line-numbers}
[root@localhost root]# group add mysql
[root@localhost root]# useradd -r -g mysql mysql
```
####1.3、初始化mysql配置表：   
```shell{.line-numbers}
[root@localhost mysql]# cd /usr/local/mysql //转到mysql目录下
[root@localhost mysql]# chown -R mysql:mysql ./ //修改当前目录为mysql用户
[root@localhost mysql]# bin/mysqld --initialize --user=mysql --basedir=/usr/local/mysql --datadir=/usr/local/mysql/data //初始化数据库 
```
初始化数据库这个命令和mysql5.7之前的命令不一样了，之前命令是：bin/mysql_install_db --user=mysql，但是之后的版本已经被mysqld --initialize替代发现mysql_install_db没有这个文件，所以在bin下创建这个文件，并且配置权限；
会提示如下，警告无所谓，这是因为我们的/etc/my.cnf文件没有做修改。关注root的密码成功创建，并执行成功，记住初始密码即可；
![](images/c8ade3b9-afee-4caa-b1ad-7800a6248476.png)

####1.4、复制或修改配置文件
```shell{.line-numbers}
[root@localhost mysql]# cp support-files/my-default.cnf /etc/my.cnf
[root@localhost mysql]# vim /etc/my.cnf
```
若没有support-files/my-default.cnf则可以自己创建一个
```shell{.line-numbers}
[root@localhost mysql]# mkdir my-default.cnf
[root@localhost mysql]# chmod 777 ./my-default.cnf
```
并修改: 或者直接修改/etc/my.cnf
```shell{.line-numbers}
[mysqld]
basedir = /usr/local/mysql   //mysql的目录
datadir = /usr/local/mysql/data  //mysql数据的目录
port = 3306 //端口
socket = /usr/local/mysql/tmp/mysql.sock //使用的连接文件
```
**必填项**
```shell{.line-numbers}
sql_mode=NO_ENGINE_SUBSTITUTION,STRICT_TRANS_TABLES
```
__【注意】:如果/usr/local/mysql/目录下没有tmp文件，手动创建，并且配置权限：__
```shell{.line-numbers}
[root@localhost mysql]# mkdir tmp
[root@localhost mysql]# chmod 777 ./tmp
```
### 2、加入开机自启项
#### 2.1、将{mysql}/ support-files/mysql.server 拷贝为/etc/init.d/mysql并设置运行权限，这样就可以使用service mysql命令启动/停止服务
```shell{.line-numbers}
[root@localhost mysql]# cp support-files/mysql.server /etc/init.d/mysql
[root@localhost mysql]# chmod +x /etc/init.d/mysql
```
#### 2.2、注册启动服务：    
```shell{.line-numbers}
[root@localhost mysql]# chkconfig --add mysql  //注册启动服务
[root@localhost mysql]# chkconfig --list mysql //查看是否添加成功
Note: This output shows SysV services only and does not include native
      systemd services. SysV configuration data might be overridden by native
      systemd configuration.
      If you want to list systemd services use 'systemctl list-unit-files'.
      To see services enabled on particular target use
      'systemctl list-dependencies [target]'.
mysql           0:off   1:off   2:on    3:on    4:on    5:on    6:off
```
#### 2.3、启动mysql服务：

```shell{.line-numbers}
[root@localhost mysql]# service mysql start
Starting MySQL.2018-09-17T15:37:21.356869Z mysqld_safe error: log-error set to 
'/var/log/mariadb/mariadb.log', however file don't exists. Create writable for user 'mysql'.
 ERROR! The server quit without updating PID file (/usr/local/mysql/data//localhost.localdomain.pid). 
 ```

上诉的的启动报错，提示找不到相关文件，这是因为我安装的CentOS 7.x 默认的是mariadb，其相关配置，使用的是mariadb.log
只需要将这两个log的相关文件修改成mysql的即可；
修改后，再次启动，若还是出现这个错误

```shell{.line-numbers}
ERROR! The server quit without updating PID file (/usr/local/mysql/data/localhost.localdomain.pid).
```

则需要查看selinux配置，并将SELINUX=enforcing 修改为 SELINUX=disabled，重启系统；

 ```shell
[root@localhost mysql]# cat /etc/selinux/config

# This file controls the state of SELinux on the system.
# SELINUX= can take one of these three values:
#     enforcing - SELinux security policy is enforced.
#     permissive - SELinux prints warnings instead of enforcing.
#     disabled - No SELinux policy is loaded.
#SELINUX=enforcing
SELINUX=disabled 
# SELINUXTYPE= can take one of three two values:
#     targeted - Targeted processes are protected,
#     minimum - Modification of targeted policy. Only selected processes are protected. 
#     mls - Multi Level Security protection.
SELINUXTYPE=targeted 
``` 
再次执行，恭喜，启动成功！
 ```shell{.line-numbers} 
[root@localhost mysql]# service mysql start
Starting MySQL.Logging to '/usr/local/mysql/data/localhost.localdomain.err'.
. SUCCESS! 
 ``` 
#### 2.4、添加环境变量
```shell{.line-numbers}
[root@localhost mysql]# vi /etc/profile
//文件最后添加
[root@localhost mysql]# export PATH=$PATH:/usr/local/mysql/bin:/usr/local/mysql/lib
//重新加载环境变量
[root@localhost mysql]# source /etc/profile
```

#### 2.5、登录并修改密码

```shell{.line-numbers}
[root@localhost mysql]# mysql -uroot -p b_clEWano6cC
ERROR 2002 (HY000): Can't connect to local MySQL server through socket '/tmp/mysql.sock' (2)
```

又出错了！！   提示该错的意思，在找了相关的解决方案，问题如下：
这是由于我们连接数据库使用的主机名参数为“localhost”，或者未使用主机名参数、服务器默认使用“localhost”做为主机名（爱Ｅ族）。 使用主机名参数为“localhost”连接mysql服务端时，mysql客户端会认为是连接本机，所以会尝试以socket文件方式进行连接(socket文件连接方式，比“ip：端口”方式效率更高)，这时根据配置文件“/etc/my.cnf”的路径，未找到相应的socket文件，就会引发此错误。
        
解决方案一：　
    修改“/etc/my.cnf”配置文件，在配置文件中添加“[client]”选项和“[mysql]”选项，并使用这两个选项下的“socket”参数值，与“[mysqld]”选项下的“socket”参数值，指向的socket文件路径完全一致。如下：

```shell{.line-numbers}
[mysqld]
datadir=/usr/local/mysql
datadir=/usr/local/mysql/data
port=3306
socket=/usr/local/mysql/tpm/mysql.sock
# Disabling symbolic-links is recommended to prevent assorted security risks
symbolic-links=0
# Settings user and group are ignored when systemd is used.
# If you need to run mysqld under a different user or group,
# customize your systemd unit file for mariadb according to the
#instructions in http://fedoraproject.org/wiki/Systemd

sql_mode=NO_ENGINE_SUBSTITUTION,STRICT_TRANS_TABLES
[client]
port=3306
socket=/usr/local/mysql/tpm/mysql.sock
[mysqld_safe]
log-error=/var/log/mysql/mysql.log
pid-file=/var/run/mysql/mysql.pid
#
# include all files from the config directory
#
!includedir /etc/my.cnf.d
``` 

修改完后，重启mysqld服务，即可解决此问题。
解决方案二：　　
    使用“ln -s /storage/db/mysql/mysql.sock /var/lib/mysql/mysql.sock”命令，将正确的socket文件位置，软链接到提示错误的socket文件路径位置，即可解决此问题：
    解决方案参考：http://aiezu.com/article/mysql_cant_connect_through_socket.html
    现在再次执行mysql登陆，终于成功了！

```shell{.line-numbers}
[root@localhost mysql]# mysql -uroot -p123456
mysql: [Warning] Using a password on the command line interface can be insecure.
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 8
Server version: 8.0.12 MySQL Community Server - GPL
​
Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
​
Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.
mysql> 
```

**修改密码：**
```shell{.line-numbers}
alter user 'root'@'localhost' identified by '123456'；
```
####2.6、远程登陆
   我使用的是Navicat Premium 12，（破解参考：https://blog.csdn.net/qq_39019865/article/details/79317075）
   配置后，登陆无法正常连接，返回10060；
   原因是我的防火墙未放行3306端口，CentOS 7.x 默认使用的的firewall防火墙，因此执行以下命令，放行3306端口；  
```shell{.line-numbers} 
[root@localhost tpm]# firewall-cmd --zone=public --add-port=3306/tcp --permanent  //permanent 永久生效
success
[root@localhost tpm]# firewall-cmd --reload //重新加载
success
```
再次登陆，错误代码是1130，ERROR 1130: Host X.X.X.X is not allowed to connect to this MySQL server；
又该如何解决呢？可以看出，这是因为某些原因限制了ip登陆。
修改mysql的账户设置。
mysql账户是否不允许远程连接。如果无法连接可以尝试以下方法：
```shell{.line-numbers}
mysql -u root -p //登录MySQL
mysql> GRANT ALL PRIVILEGES ON *.* TO 'root'@'%'WITH GRANT OPTION; //任何远程主机都可以访问数据库
mysql> FLUSH PRIVILEGES; //需要输入次命令使修改生效
mysql> EXIT //退出
```
也可以通过修改表来实现远程：
```shell{.line-numbers}
mysql -u root -p
mysql> use mysql;
mysql> update user set host = '%' where user = 'root';
mysql> select host, user from user;
mysql> FLUSH PRIVILEGES; //需要输入次命令使修改生效
```
重新再次登陆，终于OK了。