#安装
##xshell无法连接
遇到这个问题，第一个反应是否安装相应的组件：
```shell
sudo apt-get install openssh-server
```
安装成功后，应该就可以正常访问了。
若还不行，则检查防火墙；
开启防火墙
```shell
firewall-cmd --zone=public --add-port=22/tcp --permanent
```

###设置apt镜像
对于 Ubuntu ，可以通过修改 /etc/apt/sources.list 文件内容来修改 apt 源，以下以更换为 OPSX 的 Ubuntu 镜像源为例；
备份 sources.list 文件
```shell
cd /etc/apt/
sudo cp sources.list sources.list.bak
```
将 http://archive.ubuntu.com/ 部分更换为获取到的国内镜像源地址，更换后示例内容如下：
```shell
eb http://mirrors.aliyun.com/ubuntu/ bionic main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ bionic-security main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ bionic-updates main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ bionic-proposed main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ bionic-backports main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic-security main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic-updates main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic-proposed main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic-backports main restricted universe multiverse
```
