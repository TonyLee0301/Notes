#Docker 数据管理
![docker-data-manage](images/docker-data.png)
这一章介绍如何在 Docker 内部以及容器之间管理数据，在容器中管理数据主要有两种方式：
* 数据卷（Volumes）
* 挂载主机目录 (Bind mounts)

##数据卷
数据卷 是一个可供一个或多个容器使用的特殊目录，它绕过 UFS，可以提供很多有用的特性：
* 数据卷 可以在容器之间共享和重用
* 对 `数据卷` 的修改会立马生效
* 对 `数据卷` 的更新，不会影响镜像
数据卷 默认会一直存在，即使容器被删除
>注意：数据卷 的使用，类似于 Linux 下对目录或文件进行 mount，镜像中的被指定为挂载点的目录中的文件会隐藏掉，能显示看的是挂载的 数据卷。
###创建一个数据卷
```shell
$ docker volume create my-vol
```
查看所有的 数据卷
```shell
$ docker volume ls
DRIVER              VOLUME NAME
local               my-vol
```