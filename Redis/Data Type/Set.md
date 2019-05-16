#Set 集合
__Set 就是一个集合,集合的概念就是一堆不重复值的组合。利用 Redis 提供的 Set 数据结构,可以存储一些集合性的数据。__
>比如在 微博应用中,可以将一个用户所有的关注人存在一个集合中,将其所有粉丝存在一个集合。  

因为 Redis 非常人性化的为集合提供了 求交集、并集、差集等操作, 那么就可以非常方便的实现如共同关注、共同喜好、二度好友等功能, 对上面的所有集合操作,你还可以使用不同的命令选择将结果返回给客户端还是存集到一个新的集合中。

#SADD
`SADD key member [member ...]`  

__添加一个或多个指定的member元素到集合的 key中__
```shell
127.0.0.1:6379> sadd set "hello"
(integer) 1
127.0.0.1:6379> sadd set "world"
(integer) 1
```
#SCARD
`SCARD key`
```shell
127.0.0.1:6379> SCARD set
(integer) 2
```

#SDIFF
`SDIFF key [key ...]`  

__返回一个集合与给定集合的差集的元素.__
```shell
127.0.0.1:6379> SDIFF set myset
1) "world"
2) "hello"
127.0.0.1:6379> SDIFF myset set
1) "a"
2) "b"
3) "c"
```