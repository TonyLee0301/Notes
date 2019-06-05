#Sorted Set 有序集合
&emsp;&emsp;Redis 有序集合和集合一样也是string类型元素的集合,且不允许重复的成员。
&emsp;&emsp;不同的是每个元素都会关联一个double类型的分数。redis正是通过分数来为集合中的成员进行从小到大的排序。
&emsp;&emsp;有序集合的成员是唯一的,但分数(score)却可以重复。
&emsp;&emsp;集合是通过哈希表实现的，所以添加，删除，查找的复杂度都是O(1)。 集合中最大的成员数为 2^32^ - 1 (4294967295, 每个集合可存储40多亿个成员)。


##ZADD
`ZADD key [NX|XX] [CH] [INCR] score member [score member ...]`  

将一个或多个 member 元素及其 score 值加入到有序集 key 当中
```shell
127.0.0.1:6379> zadd myset 1 "one"
(integer) 1
127.0.0.1:6379> zadd myset 2 "two"
(integer) 1
```