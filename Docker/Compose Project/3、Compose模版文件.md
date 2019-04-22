#Compose 模板文件
模板文件是使用 Compose 的核心，涉及到的指令关键字也比较多。但大家不用担心，这里面大部分指令跟 `docker run` 相关参数的含义都是类似的。

默认的模板文件名称为 `docker-compose.yml`，格式为 YAML 格式。
```yml
version: "3"

services:
  webapp:
    image: examples/web
    ports:
      - "80:80"
    volumes:
      - "/data"
```
注意每个服务都必须通过 `image` 指令指定镜像或 `build` 指令（需要 Dockerfile）等来自动构建生成镜像。

如果使用 `build` 指令，在 `Dockerfile` 中设置的选项(例如：`CMD`, `EXPOSE`, `VOLUME`, `ENV` 等) 将会自动被获取，无需在 docker-compose.yml 中再次设置。

下面分别介绍各个指令的用法。
指定 `Dockerfile` 所在文件夹的路径（可以是绝对路径，或者相对 `docker-compose.yml` 文件的路径）。 `Compose` 将会利用它自动构建这个镜像，然后使用这个镜像。
```yml
version: '3'
services:

  webapp:
    build: ./dir
```
你也可以使用 `context` 指令指定 `Dockerfile` 所在文件夹的路径。

使用 `dockerfile` 指令指定 `Dockerfile` 文件名。

使用 `arg` 指令指定构建镜像时的变量。
```yml
version: '3'
services:

  webapp:
    build:
      context: ./dir
      dockerfile: Dockerfile-alternate
      args:
        buildno: 1
```
使用 `cache_from` 指定构建镜像的缓存
```yml
build:
  context: .
  cache_from:
    - alpine:latest
    - corp/web_app:3.14
```
`cap_add, cap_drop`
指定容器的内核能力（`capacity`）分配。

例如，让容器拥有所有能力可以指定为：
```shell
cap_add:
  - ALL
```
去掉 NET_ADMIN 能力可以指定为：
```shell
cap_drop:
  - NET_ADMIN
```