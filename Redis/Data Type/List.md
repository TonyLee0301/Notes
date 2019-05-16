#List 列表
__Redis列表是简单的字符串列表，按照插入顺序排序。你可以添加一个元素导列表的头部（左边）或者尾部（右边）
一个列表最多可以包含 2^32^ - 1 个元素 (4294967295, 每个列表超过40亿个元素)。__

#LPUSH
`LPUSH key value [value ...]`  

__在list头部插入所有指定的值__
```shell
127.0.0.1:6379> LPUSH headlist "10" "9" "8"
(integer) 3
127.0.0.1:6379> LRANGE headlist 0 -1
1) "8"
2) "9"
3) "10"
127.0.0.1:6379> LPUSH headlist "1"
(integer) 4
127.0.0.1:6379> LRANGE headlist 0 -1
1) "1"
2) "8"
3) "9"
4) "10"
```

##RPUSH
`RPUSH key value [value ...]`  

__向存于 key 的列表的尾部插入所有指定的值。如果 key 不存在，那么会创建一个空的列表然后再进行 push 操作。__
```shell
127.0.0.1:6379> RPUSH list "hello"
(integer) 1
127.0.0.1:6379> RPUSH list "world"
(integer) 2
127.0.0.1:6379> RPUSH list "I am Jemmy"
(integer) 3
```

##LLEN
`LLEN key`  

__获取这个list的长度__
```shell
127.0.0.1:6379> LLEN list
(integer) 3
```

##LRANGE
`LRANGE key start stop`  

__获取这个范围的list值__
start 和 end 也可以用负数来表示与表尾的偏移量，比如 -1 表示列表里的最后一个元素， -2 表示倒数第二个，等等。
```shell
127.0.0.1:6379> LRANGE list 0 10
1) "hello"
2) "world"
3) "I am Jemmy"
```

##LPOP
`LPOP key`  

__移除并且返回 key 对应的 list 的第一个元素。__
```shell
127.0.0.1:6379> RPUSH mylist "one" "two" "three"
(integer) 3
127.0.0.1:6379> RPUSH mylist "four"
(integer) 4
127.0.0.1:6379> LPOP mylist
"one"
127.0.0.1:6379> LLEN mylist
(integer) 3
127.0.0.1:6379> LRANGE mylist 0 10
1) "two"
2) "three"
3) "four"
```

##RPOP
`RPOP key`  

__移除并返回 key 对应的 list 的最后一个元素__
```shell
127.0.0.1:6379> RPOP mylist
"four"
127.0.0.1:6379> LRANGE mylist 0 -1
1) "two"
2) "three"
```

##LTRIM
`LTRIM key start stop`  

__修剪(trim)一个已存在的 list，这样 list 就会只包含指定范围的指定元素。__
>start 和 stop 都是由0开始计数的， 这里的 0 是列表里的第一个元素（表头），1 是第二个元素，以此类推。
例如： LTRIM foobar 0 2 将会对存储在 foobar 的列表进行修剪，只保留列表里的前3个元素。
start 和 end 也可以用负数来表示与表尾的偏移量，比如 -1 表示列表里的最后一个元素， -2 表示倒数第二个，等等。
```shell
127.0.0.1:6379> LRANGE list 0 -1
1) "hello"
2) "world"
3) "I am Jemmy"
127.0.0.1:6379> LRANGE list 0 -1
1) "hello"
2) "world"
3) "I am Jemmy"
127.0.0.1:6379> LTRIM list 0 1
OK
127.0.0.1:6379> LRANGE list 0 -1
1) "hello"
2) "world"
```

##应用场景
1.取最新N个数据的操作
比如典型的取你网站的最新文章，通过下面方式，我们可以将最新的5000条评论的ID放在Redis的List集合中，并将超出集合部分从数据库获取
* 使用LPUSH latest.comments命令，向list集合中插入数据
* 插入完成后再用LTRIM latest.comments 0 5000命令使其永远只保存最近5000个ID
* 然后我们在客户端获取某一页评论时可以用下面的逻辑（伪代码）
```
FUNCTION get_latest_comments(start,num_items):
  id_list = redis.lrange("latest.comments",start,start+num_items-1)
  IF id_list.length < num_items
      id_list = SQL_DB("SELECT ... ORDER BY time LIMIT ...")
  END
  RETURN id_list
```
如果你还有不同的筛选维度，比如某个分类的最新N条，那么你可以再建一个按此分类的List，只存ID的话，Redis是非常高效的。

##示例
取最新N个评论的操作
```shell
127.0.0.1:6379> lpush mycomment 100001
(integer) 1
127.0.0.1:6379> lpush mycomment 100002
(integer) 2
127.0.0.1:6379> lpush mycomment 100003
(integer) 3
127.0.0.1:6379> lpush mycomment 100004
(integer) 4
127.0.0.1:6379> LRANGE mycomment 0 -1
1) "100004"
2) "100003"
3) "100002"
4) "100001"
127.0.0.1:6379> LTRIM mycomment 0 1
OK
127.0.0.1:6379> LRANGE mycomment 0 -1
1) "100004"
2) "100003"
127.0.0.1:6379> lpush mycomment 100005
(integer) 3
127.0.0.1:6379> LRANGE mycomment 0 -1
1) "100005"
2) "100004"
3) "100003"
127.0.0.1:6379> LTRIM mycomment 0 1
OK
127.0.0.1:6379> LRANGE mycomment 0 -1
1) "100005"
2) "100004"
127.0.0.1:6379>
```
