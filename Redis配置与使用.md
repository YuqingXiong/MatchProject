[Centos7 安装Redis详细教程 - JcongJason - 博客园 (cnblogs.com)](https://www.cnblogs.com/jiangcong/p/15449452.html)

# 安装

## 下载redis安装包并解压

```sh
# 下载，我是在root下执行的下载，所以我的下载目录为：/root/redis-5.0.5，这里按照自己的实际情况调整
wget https://download.redis.io/releases/redis-5.0.5.tar.gz
# 解压
tar -zxvf redis-5.0.5.tar.gz
```

![image-20240417100640156](https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404171006210.png)

## 进入解压目录并编译

```sh
# 进入解压目录
cd redis-5.0.5
# 编译
make
```

## 指定安装目录并进行安装

```sh
make install PREFIX=/usr/local/redis
```

# 启动

redis 路径：`usr/local/redis` 

![image-20240417100319081](https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404171004967.png)

## 手动启动：通过守护进程方式启动

1. 修改配置文件内容

```sh
# 修改redis.conf配置文件
cd /usr/local/redis/
vi redis.conf
```

修改内容如下：

```sh
# daemonize 的值从 no 修改成 yes（Redis服务默认是前台运行，需要修改为后台运行）：
daemonize yes

# bind 为 bind 0.0.0.0 这样redis就可以接受其他主机连接
# Redis默认只支持本地链接，修改保护模式为 no 
bind 127.0.0.1
protected-mode no
```

2. 启动：

```sh
# 启动服务
./redis-server redis.conf
# 查看进程来确定redis是否启动成功，非必须
ps -ef |grep redis
```

## 设置开机自动启动

1. 修改配置文件内容

```sh
# 修改redis.conf配置文件
cd /usr/local/redis/
vi redis.conf
```

修改内容如下：

```sh
# daemonize 的值从 no 修改成 yes（Redis服务默认是前台运行，需要修改为后台运行）：
daemonize yes

# bind 为 bind 0.0.0.0 这样redis就可以接受其他主机连接
# Redis默认只支持本地链接，修改保护模式为 no 
bind 127.0.0.1
protected-mode no
```

2. 创建redis.service 服务

  切换到/lib/systemd/system/目录，创建redis.service文件 ：

```sh
cd /lib/systemd/system/
vim redis.service
```

填写文件内容，设置 **redis 的启动地址**，和**配置文件地址**

```sh
[Unit]
Description=redis-server
After=network.target

[Service]
Type=forking
# ExecStart需要按照实际情况修改成自己的地址
ExecStart=/usr/local/redis/src/redis-server /usr/local/redis/redis.conf
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

3. 设置开启自动启动

```sh
# 开机自动启动
systemctl enable redis.service
# 启动redis服务
systemctl start redis.service
# 查看服务状态
systemctl status redis.service
```

其他

```sh
# 停止服务
systemctl stop redis.service
# 取消开机自动启动(卸载服务)
systemctl disabled redis.service
```

# 使用

## windows 客户端远程连接 centos7 中的 Redis

[史上最全：windows电脑连接虚拟机(Linux)上的redis教程_windows连接虚拟机redis-CSDN博客](https://blog.csdn.net/zuodingquan666/article/details/118972649)

1. 关闭防火墙

```sh
#设置开机禁用防火墙：
systemctl disable firewalld.service

#关闭防火墙：
systemctl stop firewalld

#检查防火墙状态：
systemctl status firewalld
```

其他：

```sh
#设置开机启用防火墙：
systemctl enable firewalld.service

#启动防火墙：
systemctl start firewalld
```

2. 查看ip 地址后连接

![image-20240417112345871](https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404171123006.png)

默认端口号：6379

![image-20240417112430663](https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404171124071.png)

## windows IDEA 中使用 Redis

执行 `windows 客户端远程连接 centos7 中的 Redis` 目录中的步骤，然后：

1. 安装包

选择包的版本（和springboot版本保持一致）：[Maven Repository: org.springframework.boot » spring-boot-starter-data-redis (mvnrepository.com)](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-data-redis)

这里选择3.2.2

```xml
<!-- https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-data-redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <version>3.2.2</version>
</dependency>
```

2. 配置 redis

```yml
spring:
  # redis 配置
  data:
    redis:
      port: 6379
      host: 192.168.137.133 # centos 的 ip地址
      database: 0
```

3. 引入spring-session-redis的整合

可以自动将 session 存储到 redis 中

```xml
<!-- https://mvnrepository.com/artifact/org.springframework.session/spring-session-data-redis -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
    <version>3.2.2</version>
</dependency>
```

> springboot2.X 中需要修改 session 配置存储类型：session.store-type: redis

springboot3.X 不需要修改配置：[Spring Boot 3.0 迁移指南 ·spring-projects/spring-boot 维基 (github.com)](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide#spring-session-store-type)

提供了自动配置顺序：[web.spring-session(spring.io)](https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/web.html#web.spring-session)

4. 读写 session

```java
// 记录用户的登录态
request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, safetyUser);

// 获取当前用户信息
User userObject = (User)request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
```

get set session 自动转化为 redis 中存储

![image-20240417131831245](https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404171318344.png)

