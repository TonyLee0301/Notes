<style>
body{
    width:100%;
}
table {
    width: 100%; /*表格宽度*/
    max-width: 90em; /*表格最大宽度，避免表格过宽*/
    border: 1px solid #dedede; /*表格外边框设置*/
    margin: 15px auto; /*外边距*/
    border-collapse: collapse; /*使用单一线条的边框*/
    empty-cells: show; /*单元格无内容依旧绘制边框*/
}

table th,
table td {
  height: 35px; /*统一每一行的默认高度*/
  border: 1px solid #dedede; /*内部边框样式*/
  padding: 0 10px; /*内边距*/
}
table th {
    font-weight: bold; /*加粗*/
    text-align: center !important; /*内容居中，加上 !important 避免被 Markdown 样式覆盖*/
    background: #efefef; /*背景色*/
}
table tbody tr:nth-child(2n) {
    background: rgba(158,188,226,0.12); 
}
table th:nth-of-type(1) {
	width: 5em;
}
table th:nth-of-type(2) {
	width: 40em;
}
table th:nth-of-type(3) {
	width: 40em;
}
table tr:hover {
    background: #efefef; 
}
table td:nth-child(1) {
    white-space: nowrap; 
}
</style>
#Redis命令集
##字符串
>String 是redis最基本的类型，value 不仅可以是 String,也可以是数字。
使用 Strings 类型,可以完全实现目前 Memcached 的功能,并且效率更高。还可以享受 Redis 的定时持久化(可以选择 [RDB 模式]或者 AOF 模式).
string类型是二进制安全的。意思是redis的string可以包含任何数据,比如jpg图片或者序列化的对象
string类型是Redis最基本的数据类型，一个键最大能存储512MB。

##字符串类型相关命令

命令 | 格式 | 说明 
:---: | :-- | :--- 
[__SET__](#SET) | `SET key value [expiration EX seconds|PX milliseconds] [NX|XX]` | 设置key对应的值为string类型的value。
[__GET__](#GET)| `GET key` | 获取某个key
[__SETNX__](#SETNX)| `SETNX key value` | 将key设置值为value，如果key不存在，这种情况下等同SET命令。 </br>当key存在时，什么也不做。SETNX是”SET If Not Exits”的简写。
[__SETEX__](#SETEX) | `SETEX key seconds value` | 设置key对应字符串value，并且设置key在给定的seconds时间之后超时过期。
[__SETRANGE__](#SETRANGE) | `SETRANGE key offset value` | 覆盖key对应的string的一部分，从指定的offset处开始，覆盖value的长度。
[__STRLEN__](#STRLEN) | `STRLEN key` | 获取该key value的长度
[__MSET__](#MSET) | `MSET key value [key value ...]`| 一次设置多个key的值,成功返回ok表示所有的值都设置了,失败返回0表示没有任何值被设置。
[__MGET__](#MGET) |`MGET key [key ...]`|一次获取多个key的值,如果对应key不存在,则对应返回nil
[__MSETNX__](#MSETNX) |`MSETNX key value [key value ...]`| 对应给定的keys到他们相应的values上。只要有一个key已经存在，MSETNX一个操作都不会执行。</br>MSETNX是原子的，所以所有给定的keys是一次性set的
[__GETSET__](#GETSET)|`GETSET key value`|设置key的值,并返回key的旧值
[__GETRANGE__](#GETRANGE) | `GETRANGE key start end`| 获取指定key的value值的子字符串。是由start和end位移决定的
[__INCR__](#INCR) | `INCR key` | 对key的值加1操作
[__INCRBY__](#INCRBY) | `INCRBY key increment` | 同incr类似,加指定值 ,key不存在时候会设置key,并认为原来的value是 0
[__DECR__](#DECR) | `DECR key` | 对key的值减一操作
[__DECRBY__](#DECRYBY) | `DECRBY key decrement` | 同decr,减指定值
[__APPEND__](#APPEND) | `APPEND key value` | 给指定key的字符串值追加value,返回新字符串值的长度

##HASH 哈希


###SET 
__设置key对应的值为string类型的value__
```shell
 set name "hello redis"
OK
> get name
"hello redis"
```

###SENX
**将key设置值为value，如果key不存在，这种情况下等同SET命令。当key存在时，什么也不做。SETNX是”SET if Not eXists”的简写。**
```shell
> setnx name "hello redis nx"
(integer) 0
> get name
"hello redis"
```

###SETEX
__设置key对应字符串value，并且设置key在给定的seconds时间之后超时过期。__
```shell
> setex color 5 "red"
OK
##等待5秒后
> get color
(nil)
```

###SETRANGE
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

###STRLEN
__获取value是字符串的长度__
```shell
127.0.0.1:6379> strlen email
(integer) 17
```

###MSET
__一次设置多个key的值,成功返回ok表示所有的值都设置了,失败返回0表示没有任何值被设置。__
```shell
127.0.0.1:6379> mset key1 value1 key2 value2
OK
```

###MGET
__一次获取多个key的值,如果对应key不存在,则对应返回nil__
```shell
127.0.0.1:6379> mget key1 key2
1) "value1"
2) "value2"
```

###MSETNX
__对应给定的keys到他们相应的values上。只要有一个key已经存在，MSETNX一个操作都不会执行。  
MSETNX是原子的，所以所有给定的keys是一次性set的__
```shell
127.0.0.1:6379> msetnx key1 hello key3 world
(integer) 0
```

###GETSET
__设置key的值,并返回key的旧值__
```shell
127.0.0.1:6379> getset name "hello world"
"hello redis"
127.0.0.1:6379> get name
"hello world"
```

###GETRANGE
__获取指定key的value值的子字符串。是由start和end位移决定的__
```shell
127.0.0.1:6379> get name
"hello world"
127.0.0.1:6379> getrange name 6 10
"world"
```

###INCR
__对key的值加1操作__
```shell
127.0.0.1:6379> set age 20
OK
127.0.0.1:6379> incr age
(integer) 21
```
###INCRBY
__同incr类似,加指定值 ,key不存在时候会设置key,并认为原来的value是 0__
```shell
127.0.0.1:6379> incrby age 20
(integer) 41
127.0.0.1:6379> incrby online 10
(integer) 10
```

###DECR 
__对key的值做的是减减操作,decr一个不存在key,则设置key为­1__
```shell
127.0.0.1:6379> decr age
(integer) 40
```


###DECRBY
__同decr,减指定值__
```shell
127.0.0.1:6379> decrby age 20
(integer) 20
```

###APPEND
__给指定key的字符串值追加value,返回新字符串值的长度__
```shell
127.0.0.1:6379> append name " redis"
(integer) 17
127.0.0.1:6379> get name
"hello world redis"
```
