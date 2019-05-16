#HASH 哈希
**Redis hash 是一个string类型的field和value的映射表，hash特别适合用于存储对象。
Redis 中每个 hash 可以存储 2^32^ - 1 键值对（40多亿）。**

##HSET
`HSET key field value`  

__设置 key 指定的哈希集中指定字段的值__
```shell
127.0.0.1:6379> hset myhash filed1 Hello
(integer) 1
```

##HGET
`HGET key field`  

__获取指定的hash field值__
```shell
127.0.0.1:6379> hget myhash filed1
"Hello"
127.0.0.1:6379> hget myhash filed3
(nil)
127.0.0.1:6379> hget myhash1 filed1
(nil)
```
由于myhash数据库没有field3,所以取到的是一个空值nil.
由于没有myhash1数据库，所以取到的是一个空值nil.

##HSETNX 
`HSETNX key field value`  
  
__只在 key 指定的哈希集中不存在指定的字段时，设置字段的值。如果 key 指定的哈希集不存在，会创建一个新的哈希集并与 key 关联。如果字段已存在，该操作无效果。__
```shell
127.0.0.1:6379> hsetnx myhash field "Hello"
(integer) 1
127.0.0.1:6379> hsetnx myhash field "Hello"
(integer) 0
```
第一次执行是成功的,但第二次执行相同的命令失败,原因是field已经存在了。

##HMSET
`HMSET key field value [field value ...]`  

__同时设置hash的多个field。__
```shell
127.0.0.1:6379> hmset myhash field1 Hello field2 World
OK
```

##HMGET
`HMGET key field [field ...]`  
__获取全部指定的hash filed。__
```shell
127.0.0.1:6379> hmget myhash filed1 field2 filed3
1) "Hello"
2) "World"
3) (nil)
```


##HINCRBY
`HINCRBY key field increment`  

__指定的hash filed 加上给定值。__
```shell
127.0.0.1:6379> hset myhash filed3 20
(integer) 1
127.0.0.1:6379> hget myhash filed3
"20"
127.0.0.1:6379> hincrby myhash filed3 -8
(integer) 12
```
##HEXISTS
`HEXISTS key field`  

__测试指定field是否存在。__
```shell
127.0.0.1:6379> hexists myhash field1
(integer) 1
127.0.0.1:6379> hexists myhash filed49
(integer) 0
```
存在返回1，不存在返回0

##HKEYS
`HKEYS key` ­­ 

__返回hash的所有field。__
```shell
127.0.0.1:6379> hkeys myhash
1) "field"
2) "field1"
3) "field2"
4) "filed3"
```

##HLEN
`HLEN key`  

__返回指定hash的field数量。__
```shell
127.0.0.1:6379> hlen myhash
(integer) 4
```

##HDEL
`HDEL key field [field ...]`  

__从 key 指定的哈希集中移除指定的域__
```shell
127.0.0.1:6379> hdel myhash field
(integer) 1
127.0.0.1:6379> hkeys myhash
1) "field1"
2) "field2"
3) "filed3"
```

##HVALS
`HVALS key`  

__返回hash的所有value。__
```shell
127.0.0.1:6379> hvals myhash
1) "Hello"
2) "World"
3) "12"
```

##HGETALL
`HGETALL key`  

__获取某个hash中全部的filed及value。__
```shell
127.0.0.1:6379> hgetall myhash
1) "field1"
2) "Hello"
3) "field2"
4) "World"
5) "filed3"
6) "12"
```

##HSTRLEN
`HSTRLEN key field`  

__返回 hash指定field的value的字符串长度__
```shell
127.0.0.1:6379> hstrlen myhash field1
(integer) 5
```

