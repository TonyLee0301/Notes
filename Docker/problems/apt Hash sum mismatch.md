##Hash sum mismatch这个错误是什么意思？
>apt repository metadata is organized in such a way that a top level file contains the checksums of other pieces of repository metadata.
 The “Hash sum mismatch” error indicates to the user that apt has run a checksum against the repository metadata it has downloaded and the checksum apt computed does not match the checksum listed in the top level file.
Unfortunately, due to a bug in apt, metadata files compressed with lzma (.xz files) are occasionally downloaded (and in some cases) decompressed incorrectly resulting in a broken file.     As a result, the checksum of the broken file will be incorrect and cause apt to produce the “Hash sum mismatch” error.

##Hash sum mismatch 为何产生？
>There are at least 3 ways this can happen for most Ubuntu and Debian based systems today:
>1. Stale metadata cached between the client and server. This is unlikely in most cases and not possible if SSL is used.
>1. The metadata does not match because of a bug during the extraction of the metadata.
>1. The repository is being updated while an apt-get update is run, or apt has cached a stale Release file.

##Users can avoid all 3 cases by: （如何避免？）
>1. Using SSL.
>1. Disabling XZ compressed metadata, or ensuring a newer version of APT is used.
>1. Using the new Acquire-by-hash feature available in APT 1.2.0.

来自参考文件链接:
[Solution reference4](https://blog.packagecloud.io/eng/2016/09/27/fixing-apt-hash-sum-mismatch-consistent-apt-repositories/)
[Solution reference5](https://blog.packagecloud.io/eng/2016/03/21/apt-hash-sum-mismatch/)

解决问题：
__1. 清cache缓存：__
```shell
$ sudo apt-get clean
$ sudo apt-get update --fix-missing
```
__2. 删除/var/lib/apt/lists/partial/中的下载文件：##__
```shell
$ sudo rm -R /var/lib/apt/lists/partial/*
$ sudo apt-get update && sudo apt-get upgrade
```
__3. 更换apt更新源：__
```shell
sudo cp /etc/apt/sources.list /etc/apt/sources.list.bak
```
```shell
sudo gedit /etc/apt/sources.list
```
```shell
# 网易新开的更新源
deb http://mirrors.163.com/ubuntu/ intrepid main restricted universe multiverse
deb http://mirrors.163.com/ubuntu/ intrepid-security main restricted universe multiverse
deb http://mirrors.163.com/ubuntu/ intrepid-updates main restricted universe multiverse
deb http://mirrors.163.com/ubuntu/ intrepid-proposed main restricted universe multiverse
deb http://mirrors.163.com/ubuntu/ intrepid-backports main restricted universe multiverse
deb-src http://mirrors.163.com/ubuntu/ intrepid main restricted universe multiverse
deb-src http://mirrors.163.com/ubuntu/ intrepid-security main restricted universe multiverse
deb-src http://mirrors.163.com/ubuntu/ intrepid-updates main restricted universe multiverse
deb-src http://mirrors.163.com/ubuntu/ intrepid-proposed main restricted universe multiverse
deb-src http://mirrors.163.com/ubuntu/ intrepid-backports main restricted universe multiverse

# 或者， 加入如下内容（中科大的）：
deb http://mirrors.ustc.edu.cn/ubuntu/ precise-updates main restricted
 deb-src http://mirrors.ustc.edu.cn/ubuntu/ precise-updates main restricted
deb http://mirrors.ustc.edu.cn/ubuntu/ precise universe
deb-src http://mirrors.ustc.edu.cn/ubuntu/ precise universe
deb http://mirrors.ustc.edu.cn/ubuntu/ precise-updates universe
deb-src http://mirrors.ustc.edu.cn/ubuntu/ precise-updates universe
deb http://mirrors.ustc.edu.cn/ubuntu/ precise multiverse
deb-src http://mirrors.ustc.edu.cn/ubuntu/ precise multiverse
deb http://mirrors.ustc.edu.cn/ubuntu/ precise-updates multiverse
deb-src http://mirrors.ustc.edu.cn/ubuntu/ precise-updates multiverse
deb http://mirrors.ustc.edu.cn/ubuntu/ precise-backports main restricted universe multiverse
deb-src http://mirrors.ustc.edu.cn/ubuntu/ precise-backports main restricted universe multiverse
```

__docker中出现该问题__
可修改创建文件为：
```shell
RUN sed -i s@/archive.ubuntu.com/@/mirrors.ustc.edu.cn/@g /etc/apt/sources.list && rm -Rf /var/lib/apt/lists/* && apt-get -y update && apt-get install -y **
```
本质上说和上诉的解决方案是一致的
