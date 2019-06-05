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

__返回集合存储的key的基数 (集合元素的数量)__
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

#SRANDMEMBER
`SRANDMEMBER key [count]`

__从集合中随机获取一个或多个值__
```shell
127.0.0.1:6379> SRANDMEMBER myset 2
1) "b"
2) "c"
127.0.0.1:6379> SRANDMEMBER myset 2
1) "a"
2) "b"
```

#应用场景
1. 共同好友、二度好友
2. 利用唯一性,可以统计访问网站的所有独立 IP
3. 好友推荐的时候,根据 tag 求交集,大于某个 临界值 就可以推荐

#示例
以王宝强和马蓉为例，求二度好友，共同好友，推荐系统

```shell
127.0.0.1:6379> sadd marong_friend 'songdan' 'wangsicong' 'songzhe'
(integer) 1
127.0.0.1:6379> SMEMBERS marong_friend
1) "songzhe"
2) "wangsicong"
3) "songdandan"
127.0.0.1:6379> sadd wangbaoqiang_friend 'dengchao' 'angelababy' 'songzhe'
(integer) 1

#求共同好友
127.0.0.1:6379> SINTER marong_friend wangbaoqiang_friend
1) "songzhe"

#推荐好友系统
127.0.0.1:6379> SDIFF marong_friend wangbaoqiang_friend
1) "wangsicong"
2) "songdandan"
127.0.0.1:6379>
```