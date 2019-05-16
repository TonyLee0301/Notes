#字符串
>String 是redis最基本的类型，value 不仅可以是 String,也可以是数字。
使用 Strings 类型,可以完全实现目前 Memcached 的功能,并且效率更高。还可以享受 Redis 的定时持久化(可以选择 [RDB 模式]或者 AOF 模式).
string类型是二进制安全的。意思是redis的string可以包含任何数据,比如jpg图片或者序列化的对象
string类型是Redis最基本的数据类型，一个键最大能存储512MB。

##SET 
`SET key value [expiration EX seconds|PX milliseconds] [NX|XX]`  

__设置key对应的值为string类型的value__
```shell
 set name "hello redis"
OK
> get name
"hello redis"
```

##SENX
`SETNX key value`  

**将key设置值为value，如果key不存在，这种情况下等同SET命令。当key存在时，什么也不做。SETNX是”SET if Not eXists”的简写。**
```shell
> setnx name "hello redis nx"
(integer) 0
> get name
"hello redis"
```

##SETEX
`SETEX key seconds value`  

__设置key对应字符串value，并且设置key在给定的seconds时间之后超时过期。__
```shell
> setex color 5 "red"
OK
##等待5秒后
> get color
(nil)
```

##SETRANGE
`SETRANGE key offset value`  

__覆盖key对应的string的一部分，从指定的offset处开始，覆盖value的长度。__
```shell
127.0.0.1:6379> set email tonylee@163.com
OK
127.0.0.1:6379> get email
"tonylee@163.com"
127.0.0.1:6379> setrange email 8 gmail.com
(integer) 17
127.0.0.1:6379> get email
"tonylee@gmail.com"
```

##STRLEN
`STRLEN key`  

__获取value是字符串的长度__
```shell
127.0.0.1:6379> strlen email
(integer) 17
```

##MSET
`MSET key value [key value ...]`  

__一次设置多个key的值,成功返回ok表示所有的值都设置了,失败返回0表示没有任何值被设置。__
```shell
127.0.0.1:6379> mset key1 value1 key2 value2
OK
```

##MGET
`MGET key [key ...]`  

__一次获取多个key的值,如果对应key不存在,则对应返回nil__
```shell
127.0.0.1:6379> mget key1 key2
1) "value1"
2) "value2"
```

##MSETNX
`MSETNX key value [key value ...]`  

__对应给定的keys到他们相应的values上。只要有一个key已经存在，MSETNX一个操作都不会执行。  
MSETNX是原子的，所以所有给定的keys是一次性set的__
```shell
127.0.0.1:6379> msetnx key1 hello key3 world
(integer) 0
```

##GETSET
`GETSET key value`  

__设置key的值,并返回key的旧值__
```shell
127.0.0.1:6379> getset name "hello world"
"hello redis"
127.0.0.1:6379> get name
"hello world"
```

##GETRANGE
`GETRANGE key start end`  

__获取指定key的value值的子字符串。是由start和end位移决定的__
```shell
127.0.0.1:6379> get name
"hello world"
127.0.0.1:6379> getrange name 6 10
"world"
```

##INCR
`INCR key`  

__对key的值加1操作__
```shell
127.0.0.1:6379> set age 20
OK
127.0.0.1:6379> incr age
(integer) 21
```
##INCRBY
`INCRBY key increment`  

__同incr类似,加指定值 ,key不存在时候会设置key,并认为原来的value是 0__
```shell
127.0.0.1:6379> incrby age 20
(integer) 41
127.0.0.1:6379> incrby online 10
(integer) 10
```

##DECR 
`DECR key`  

__对key的值做的是减减操作,decr一个不存在key,则设置key为­1__
```shell
127.0.0.1:6379> decr age
(integer) 40
```


##DECRBY
`DECRBY key decrement`  

__同decr,减指定值__
```shell
127.0.0.1:6379> decrby age 20
(integer) 20
```

##APPEND
`APPEND key value`  

__给指定key的字符串值追加value,返回新字符串值的长度__
```shell
127.0.0.1:6379> append name " redis"
(integer) 17
127.0.0.1:6379> get name
"hello world redis"
```