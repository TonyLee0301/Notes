#安装Docker
Docker 分为 CE 和 EE 两大版本。CE 即社区版（免费，支持周期 7 个月），EE 即企业版，强调安全，付费使用，支持周期 24 个月。

Docker CE 分为 stable, test, 和 nightly 三个更新频道。每六个月发布一个 stable 版本 (18.09, 19.03, 19.09...)。

官方网站上有各种环境下的 [安装指南](https://docs.docker.com/install/)，这里主要介绍 Docker CE 在 Linux 、 Windows 10 (PC) 和 macOS 上的安装。

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