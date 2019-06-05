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
##[字符串类型相关命令](Data%20Type/String.md)

命令 | 格式 | 说明 
:---: | :-- | :--- 
__SET__ | `SET key value [expiration EX seconds|PX milliseconds] [NX|XX]` | 设置key对应的值为string类型的value。
__GET__ | `GET key` | 获取某个key
__SETNX__ | `SETNX key value` | 将key设置值为value，如果key不存在，这种情况下等同SET命令。 </br>当key存在时，什么也不做。SETNX是”SET If Not Exits”的简写。
__SETEX__ | `SETEX key seconds value` | 设置key对应字符串value，并且设置key在给定的seconds时间之后超时过期。
__SETRANGE__ | `SETRANGE key offset value` | 覆盖key对应的string的一部分，从指定的offset处开始，覆盖value的长度。
__STRLEN__ | `STRLEN key` | 获取该key value的长度
__MSET__ | `MSET key value [key value ...]`| 一次设置多个key的值,成功返回ok表示所有的值都设置了,失败返回0表示没有任何值被设置。
__MGET__ |`MGET key [key ...]`|一次获取多个key的值,如果对应key不存在,则对应返回nil
__MSETNX__ |`MSETNX key value [key value ...]`| 对应给定的keys到他们相应的values上。只要有一个key已经存在，MSETNX一个操作都不会执行。</br>MSETNX是原子的，所以所有给定的keys是一次性set的
__GETSET__ |`GETSET key value`|设置key的值,并返回key的旧值
__GETRANGE__ | `GETRANGE key start end`| 获取指定key的value值的子字符串。是由start和end位移决定的
__INCR__ | `INCR key` | 对key的值加1操作
__INCRBY__ | `INCRBY key increment` | 同incr类似,加指定值 ,key不存在时候会设置key,并认为原来的value是 0
__DECR__ | `DECR key` | 对key的值减一操作
__DECRBY__ | `DECRBY key decrement` | 同decr,减指定值
__APPEND__ | `APPEND key value` | 给指定key的字符串值追加value,返回新字符串值的长度

##[HASH 哈希相关命令](Data%20Type/Hash.md)
命令 | 格式 | 说明 
:---: | :-- | :--- 
HSET | `HSET key field value` | 设置 key 指定的哈希集中指定字段的值
HGET | `HGET key field` | 获取指定的hash field。
HSETNX | `HSETNX key field value` | 只在 key 指定的哈希集中不存在指定的字段时，设置字段的值。如果 key 指定的哈希集不存在，会创建一个新的哈希集并与 key 关联。如果字段已存在，该操作无效果。
HMSET | `HMSET key field value [field value ...]` | 同时设置hash的多个field。
HMGET | `HMGET key field [field ...]` | 获取全部指定的hash filed。
HINCRBY | `HINCRBY key field increment` | 指定的hash filed 加上给定值。
HEXISTS | `HEXISTS key field` | 测试指定field是否存在。
HKEYS | `HKEYS key` | 返回hash的所有field。
HLEN | `HLEN key` | 返回指定hash的field数量。
HDEL | `HDEL key field [field ...]` | 从 key 指定的哈希集中移除指定的域
HVALS | `HVALS key` | ­­ 返回hash的所有value。
HGETALL | `HGETALL key` | 获取某个hash中全部的filed及value。
HSTRLEN | `HSTRLEN key field` | 返回 hash指定field的value的字符串长度

##[List 列表](Data%20Type/List.md)
命令 | 格式 | 说明 
:---: | :-- | :--- 
RPUSH | `RPUSH key value [value ...]` | 向存于 key 的列表的尾部插入所有指定的值。如果 key 不存在，那么会创建一个空的列表然后再进行 push 操作
LPUSH | `LPUSH key value [value ...]` | 在list头部插入所有指定的值
LLEN | `LLEN key` | 获取这个list的长度
LRANGE | `LRANGE key start stop` | 获取这个范围的list值
LPOP | `LPOP key` | 移除并且返回 key 对应的 list 的第一个元素。
RPOP | `RPOP key` | 移除并返回 key 对应的 list 的最后一个元素
LTRIM | `LTRIM key start stop` | 修剪(trim)一个已存在的 list，这样 list 就会只包含指定范围的指定元素。

##[Set 集合](Data%20Type/Set.md)
命令 | 格式 | 说明 
:---: | :-- | :--- 
SADD | `SADD key member [member ...]` | 添加一个或多个指定的member元素到集合的 key中
SCARD | `SCARD key` | 返回集合存储的key的基数 (集合元素的数量)
SDIFF | `SDIFF key [key ...]` | 返回一个集合与给定集合的差集的元素
SRANDMEMBER | `SRANDMEMBER key [count]` | 从集合中随机获取一个或多个值

##[Sorted Set 集合](Data%20Type/Sorted%20Set.md)
命令 | 格式 | 说明 
:---: | :-- | :--- 
ZADD | `ZADD key [NX|XX] [CH] [INCR] score member [score member ...]` | 将一个或多个值，添加到该hash-key的有序集合里，如果存在则更新