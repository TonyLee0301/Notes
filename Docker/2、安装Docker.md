
<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

* [安装Docker](#安装docker)
	* [镜像加速器](#镜像加速器)
		* [Ubuntu 14.04、Debian 7 Wheezy](#ubuntu-1404-debian-7-wheezy)
		* [Ubuntu 16.04+、Debian 8+、 CentoOS 7](#ubuntu-1604-debian-8-centoos-7)
		* [Window 10](#window-10)
		* [macOS](#macos)
		* [检查加速器是否生效](#检查加速器是否生效)

<!-- /code_chunk_output -->

#安装Docker
Docker 分为 CE 和 EE 两大版本。CE 即社区版（免费，支持周期 7 个月），EE 即企业版，强调安全，付费使用，支持周期 24 个月。

Docker CE 分为 stable, test, 和 nightly 三个更新频道。每六个月发布一个 stable 版本 (18.09, 19.03, 19.09...)。

官方网站上有各种环境下的 [安装指南](https://docs.docker.com/install/)，这里主要介绍 Docker CE 在 Linux 、 Windows 10 (PC) 和 macOS 上的安装。

##Ubuntu上安装Docker CE
>警告：切勿在没有配置 Docker APT 源的情况下直接使用 apt 命令安装 Docker.
###准备工作
####系统要求
Docker CE 支持以下版本的 Ubuntu 操作系统：
* Bionic 18.04 (LTS)
* Xenial 16.04 (LTS)
* Trusty 14.04 (LTS) (Docker CE v18.06 及以下版本)
Docker CE 可以安装在 64 位的 x86 平台或 ARM 平台上。Ubuntu 发行版中，LTS（Long-Term-Support）长期支持版本，会获得 5 年的升级维护支持，这样的版本会更稳定，因此在生产环境中推荐使用 LTS 版本。

卸载旧版本
旧版本的 Docker 称为 `docker` 或者 `docker-engine`，使用以下命令卸载旧版本：
```shell
sudo apt-get remove docker docker-engine docker.io
```
####Ubuntu 14.04 可选内核模块
从 Ubuntu 14.04 开始，一部分内核模块移到了可选内核模块包 (linux-image-extra-*) ，以减少内核软件包的体积。正常安装的系统应该会包含可选内核模块包，而一些裁剪后的系统可能会将其精简掉。AUFS 内核驱动属于可选内核模块的一部分，作为推荐的 Docker 存储层驱动，一般建议安装可选内核模块包以使用 AUFS。

如果系统没有安装可选内核模块的话，可以执行下面的命令来安装可选内核模块包：
```shell
$ sudo apt-get update
Hit:1 http://archive.ubuntu.com/ubuntu bionic InRelease
Get:2 http://archive.ubuntu.com/ubuntu bionic-updates InRelease [88.7 kB]
Get:3 http://archive.ubuntu.com/ubuntu bionic-backports InRelease [74.6 kB]                                  
Get:4 http://archive.ubuntu.com/ubuntu bionic-security InRelease [88.7 kB]                                   Get:5 http://archive.ubuntu.com/ubuntu bionic-updates/main amd64 Packages [558 kB]                           Get:6 http://archive.ubuntu.com/ubuntu bionic-updates/universe amd64 Packages [746 kB]                       Fetched 1,556 kB in 2min 27s (10.6 kB/s)                                                                     Reading package lists... Done
```

####Ubuntu 16.04 +
Ubuntu 16.04 + 上的 Docker CE 默认使用 overlay2 存储层驱动,无需手动配置。

####使用 APT 安装
由于 apt 源使用 HTTPS 以确保软件下载过程中不被篡改。因此，我们首先需要添加使用 HTTPS 传输的软件包以及 CA 证书。
```shell
$ sudo apt-get install apt-transport-https ca-certificates curl software-properties-common
```

鉴于国内网络问题，强烈建议使用国内源，官方源请在注释中查看。

为了确认所下载软件包的合法性，需要添加软件源的 GPG 密钥。

```shell
$ curl -fsSL https://mirrors.ustc.edu.cn/docker-ce/linux/ubuntu/gpg | sudo apt-key add -
OK
# 官方源
# $ curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
```
然后，我们需要向 source.list 中添加 Docker 软件源
```shell
$ sudo add-apt-repository "deb [arch=amd64] https://mirrors.ustc.edu.cn/docker-ce/linux/ubuntu $(lsb_release -cs) stable"
# 官方源
# $ sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
```
>以上命令会添加稳定版本的 Docker CE APT 镜像源，如果需要测试或每日构建版本的 Docker CE 请将 stable 改为 test 或者 nightly。

###安装 Docker CE
更新 apt 软件包缓存，并安装 docker-ce：
```shell
$ sudo apt-get update
$ sudo apt-get install docker-ce
```
使用脚本自动安装
在测试或开发环境中 Docker 官方为了简化安装流程，提供了一套便捷的安装脚本，Ubuntu 系统上可以使用这套脚本安装：
```shell
$ curl -fsSL get.docker.com -o get-docker.sh
$ sudo sh get-docker.sh --mirror Aliyun
```
###启动 Docker CE
```shell
$ sudo systemctl enable docker
$ sudo systemctl start docker
```
Ubuntu 14.04 请使用以下命令启动：
```shell
$ sudo service docker start
```
###建立 docker 用户组
默认情况下，`docker` 命令会使用 `Unix socket` 与` Docker` 引擎通讯。而只有 `root` 用户和 `docker` 组的用户才可以访问 `Docker` 引擎的 `Unix socket`。出于安全考虑，一般` Linux` 系统上不会直接使用` root` 用户。因此，更好地做法是将需要使用 `docker` 的用户加入 `docker` 用户组。

建立 docker 组：
```shell
$ sudo groupadd docker
```
将当前用户加入 docker 组：
```shell
$ sudo usermod -aG docker $USER
```
退出当前终端并重新登录，进行如下测试。
###测试 Docker 是否安装正确
```shell
$ docker run hello-world
```

---

后续做相关补充

---



```shell
$ docker run hello-world
Unable to find image 'hello-world:latest' locally
latest: Pulling from library/hello-world
1b930d010525: Pull complete
Digest: sha256:2557e3c07ed1e38f26e389462d03ed943586f744621577a99efb77324b0fe535
Status: Downloaded newer image for hello-world:latest

Hello from Docker!
This message shows that your installation appears to be working correctly.

To generate this message, Docker took the following steps:
 1. The Docker client contacted the Docker daemon.
 2. The Docker daemon pulled the "hello-world" image from the Docker Hub.
    (amd64)
 3. The Docker daemon created a new container from that image which runs the
    executable that produces the output you are currently reading.
 4. The Docker daemon streamed that output to the Docker client, which sent it
    to your terminal.

To try something more ambitious, you can run an Ubuntu container with:
 $ docker run -it ubuntu bash

Share images, automate workflows, and more with a free Docker ID:
 https://hub.docker.com/

For more examples and ideas, visit:
 https://docs.docker.com/get-started/
```
输出以上信息，则说明docker安装成功


##镜像加速器
__国内从 Docker Hub 拉取镜像有时会遇到困难，此时可以配置镜像加速器。Docker 官方和国内很多云服务商都提供了国内加速器服务，例如：__
* **Docker 官方提供的中国 registry mirror https://registry.docker-cn.com**
* **阿里云加速器(需登录账号获取) https://cr.console.aliyun.com/cn-hangzhou/mirrors**
* **七牛云加速器 https://reg-mirror.qiniu.com/**
>当配置某一个加速器地址之后，若发现拉取不到镜像，请切换到另一个加速器 地址。
国内各大云服务商均提供了 Docker 镜像加速服务，建议根据运行Docker的云平台选择对应的镜像加速服务

__我们以 Docker 官方加速器 https://registry.docker-cn.com 为例进行介绍。__
###Ubuntu 14.04、Debian 7 Wheezy
对于使用 [upstart](https://en.wikipedia.org/wiki/Upstart_(software))[^1] 的系统而言，编辑 /etc/default/docker 文件，在其中的 DOCKER_OPTS 中配置加速器地址：
```shell
DOCKER_OPTS="--registry-mirror=https://registry.docker-cn.com"
```
重新启动服务
```shell
sudo service docker restart
```
###Ubuntu 16.04+、Debian 8+、 CentoOS 7
对于使用[systemed](https://www.freedesktop.org/wiki/Software/systemd/) [^2],请在/etc/docker/daemon.json 中写入如下内容（如果文件不存在，请新建该文件）
```json
{  
    "registry-mirros":[
        "https://registry.docker-cn.com"
    ]
}
```
>注意，一定要保证该文件符合json规范，否则Docker将不能启动

之后重启服务
```shell
sudo systemctl daemon -reload
sudo systemctl restart docker
```
>注意：如果您之前查看了旧教程，修改了`docker.service`文件内容，请去掉您添加的内容(`--registry-mirror=https://registry.docker-cn.com`)，这里不再赘述。

###Window 10
对于使用 Windows 10 的系统，在系统右下角托盘 Docker 图标内右键菜单选择 `Settings` ，打开配置窗口后左侧导航菜单选择`Daemon`。在`Registry mirrors`一栏中填写加速器地址 https://registry.docker-cn.com ，之后点击`Apply`保存后 Docker就会重启并应用配置的镜像地址了

###macOS
对于使用 macOS 的用户，在任务栏点击`Docker for mac`应用图标 -> `Perferences... -> Daemon -> Registry mirrors`。在列表中填写加速器地址
https://registry.docker-cn.com 。修改完成之后，点击`Apply & Restart`按钮，Docker就会重启并应用配置的镜像地址了。

###检查加速器是否生效
命令行执行 docker info ，如果从结果中看到了如下内容，说明配置成功。
```shell
Registry Mirrors:
 https://registry.docker-cn.com/
```


[^1]:Upstart is an event-based replacement for the traditional init daemon – the method by which several Unix-like computer operating systems perform tasks when the computer is started.
Upstart是传统init守护程序的基于事件的替代 - 这是一种类似Unix的计算机操作系统在计算机启动时执行任务的方法。
[^2]:systemd is a suite of basic building blocks for a Linux system. It provides a system and service manager that runs as PID 1 and starts the rest of the system. 
上诉两种均是linux内核的启动方式（暂时自我理解）