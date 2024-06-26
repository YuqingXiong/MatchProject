# 匹配系统

将用户根据标签进行分类，匹配（交友，组队游戏）

背景：身为游戏爱好者的一员，常常为找不到同样的爱好者一起玩耍而苦恼。

方案：通过用户给自己添加的标签，使得其他人在找朋友组队时可以根据标签进行搜索分类，筛选出自己想要的队友进行精准打击


# 数据库表设计

## 标签表 tag（分类表）

性别：男，女

方向：学习，手游，端游，桌游

目标：考研、春招、秋招、考公、竞赛、玩耍

段位：初级、中级、高级

身份：大一、大二、大三、大四、学生、待业、已就业、研一、研二、研三

状态：乐观、伤心、一般、单身、已婚、有对象

**用户自定义标签**

字段：

- id: bigint主键

- 标签名: varchar 非空

- 上传标签的用户：userId bigint
- 父标签 id ： parentId bigint(分类)
- 是否为父标签：isParent tinyint(是否为父标签：0-不是 1-是)
- 创建时间 createTime, datetime
- 更新时间 updateTime, datetime
- 是否删除 isDelete, tinyint(0,1)

```mysql
create table tag
(
    id         bigint auto_increment primary key comment 'id',
    tagName    varchar(256)                       null comment '标签名称',
    userId     bigint                             null comment '用户 id',
    parentId   bigint                             null comment '父标签 id',
    isParent   tinyint                            null comment '是否为父标签：0-不是 1-是',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除'
)
    comment '标签';
```

## 修改用户表

如何增加用户的 tag 字段

1. 直接在用户表中新建一列，用 json 存储 tags 列表
    - 查询方便，没有关联表
    - 数据量大后性能低，但是可以用缓存解决（因为标签不常变化）
2. 加一个用户-tag的关联表，记录用户与标签的关系
    - 查询灵活，便于正反查询
    - 维护麻烦，影响扩展性

方式一增加 tags 字段：

```mysql
alter table user add COLUMN tags varchar(1024) null comment '标签列表';
```

增加索引：

- 为tagName添加唯一索引

![image-20240403155147148](https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404031551283.png)

- 用户Id添加普通索引：

![image-20240403155233465](https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404031552535.png)

# 后端

## 初始化

1. copy user-center 项目
2. 删除 .idea 和 .git 文件后打开项目

3. replace files : user-center 改为 rainsun-backend
4. Maven 重新加载项目
5. 初始化 git
6. 上传 github

## 后端接口

### 搜索标签：

#### Server 层

1. 允许用户传入多个标签，多个标签都存在才搜索出来  and：

   like '%Java%' and like '%C++%'

2. 允许用户传入多个标签，有任何一个标签存在就能搜索出来 or：

   like '%Java%' or like '%C++%'

两种方式：

1. SQL 查询：like 查询（一个like就需要遍历一遍，如果like很多的话就比较慢）
2. 内存查询：将数据解析成列表在内存中写程序自己查（需要将数据读到内存中进行处理，只需要查一遍数据）
3. 二者结合：先用 like 查询一两个tag对应的部分用户列表，再在内存中遍历列表对剩余的tag进行匹配

实现：

1. SQL 查询：

```java
@Override
public List<User> searchUserByTags(List<String> tagNameList){
    if(CollectionUtils.isEmpty(tagNameList)){
        throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
    for(String tagName : tagNameList){
        queryWrapper = queryWrapper.like("tags", tagName);
    }
    List<User> userList = userMapper.selectList(queryWrapper);
    return userList.stream().map(this::getSaftetyUser).collect(Collectors.toList());
}
```

2. **内存查询**

实现这种方法需要将 String 类型的 Json 描述转为对象（Json 序列化）

java json 序列化库有很多：

1. gson google (用法复杂一点)
2. fastjson alibaba (漏洞很多)
3. jackson (api功能不很丰富)
4. kryo

引入 Gson：

```xml
<!-- https://mvnrepository.com/artifact/com.google.code.gson/gson -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.8.9</version>
</dependency>
```

使用 Gson：

```java
    @Override
    public List<User> searchUserByTags(List<String> tagNameList){
        if(CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 1.查询所有用户
        List<User> userList = userMapper.selectList(queryWrapper);
        // 2. 在内存中判断是否包含要求的 tag
        Gson gson = new Gson();
        return userList.stream().filter(user -> {
            String tagStr = user.getTags();
            if(StringUtils.isBlank(tagStr)){
                return false;
            }
            Set<String> tempTagNameList = gson.fromJson(tagStr, new TypeToken<Set<String>>(){}.getType());
            for(String tagName : tagNameList){
                if(!tempTagNameList.contains(tagName)){
                    return false;
                }
            }
            return true;
        }).map(this::getSaftetyUser).collect(Collectors.toList());
    }
```

3. **优化**

- 内存查询可以多线程并发查询进行优化
- 根据数据量选择查询方法
- 数据库连接足够，内存空间足够，可以并发同时查询，谁先返回查询结果就用谁
- SQL 查询与内存查询相结合。先用SQL过滤部分tag

#### Controller 层

```java
@GetMapping("/search/tags")
public BaseResponse<List<User>> searchUserByTags(@RequestParam(required=false) List<String> tagNameList){
    if(CollectionUtils.isEmpty(tagNameList)){
        throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    List<User> userList = userService.searchUserByTags(tagNameList);

    return ResultUtils.success(userList);
}
```

**添加一个类注解，允许前端跨域访问：**`@CrossOrigin(origins = {"http://localhost:5173/"})`

![image-20240417072251458](https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404170722589.png)

## 首页推荐展示

首先，简单实现是搜索所有用户 list 列表 返回给前端：

```java
@GetMapping("/recommend")
public BaseResponse<List<User>> recommendUsers(HttpServletRequest request){
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
    List<User> userList = userService.list(queryWrapper);
    List<User> list = userList.stream().map(user -> userService.getSaftetyUser(user)).collect(Collectors.toList());
    return ResultUtils.success(list);
}
```

但是当用户量增多，这样就很慢，需要查询和传递很多数据。

优化：由于前端展示数据的数量是有限制的，一个屏幕放不了所有数据，所以可以分页返回数据

### 分页展示

使用 MyBatisPlus 的分页接口

首先需要配置 MyBatisPlus 的拦截器，添加一个分页拦截器

```java

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatisPlus 配置
 *
 */
@Configuration
@MapperScan("com.rainsun.yuqing.mapper")
public class MybatisPlusConfig {

    /**
     * 新的分页插件,一缓和二缓遵循mybatis的规则,需要设置 MybatisConfiguration#useDeprecatedExecutor = false 避免缓存出现问题(该属性会在旧插件移除后一同移除)
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

使用分页：

```java
@GetMapping("/recommend")
public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum, HttpServletRequest request){
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
    Page<User> userList = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
    return ResultUtils.success(userList);
}
```

## 定时预加载缓存-加快首页加载

首页查询用户展示即使分页了也很慢，需要几秒钟。

**缓存** ：查询数据库很慢，所以可以先预先把数据查出来，放在一个更快读取的地方，就可以不用查数据库了，这个更改读取的地方就是缓存。

**定时任务：**预加载缓存，定时更新缓存。

**分布式锁：**如果有多个机器(后台)都要执行这个定时任务吗？并不是。（分布式锁：需要控制同一时间只有一台机器去执行定时任务，其他机器不用重复执行）

## 分布式Session登录

### Session 共享

当有多个后台时，在其中一个后台A登录后，A中的session记录了用户信息，但是后台B并没有该用户的信息，就需要用户重复登录：

<img src="https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404170726945.png" alt="image-20240417072600626" style="zoom:50%;" />

为了解决这个问题，我们需要共享 Session：

<img src="https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404170726903.png" alt="image-20240417072639796" style="zoom:50%;" />

所以需要共享存储， 不能将数据放在单个后台的内存中

共享存储的方法：

1. Redis (基于K,V的数据库)。
2. MySQL
3. 文件服务器 ceph

这里使用 Redis 实现：用户信息读取/是否登录的判断很频繁，而 Redis 基于内存，读写性能高，简单数据的 QPS 可以达到 5W-10W

### Redis 实现

https://blog.csdn.net/qq_45364953/article/details/137869498

# 生成接口文档

Swagger pom 引入

```xml
<!--为了与 swagger 依赖的版本匹配，这里指定了版本-->
<dependency>
    <groupId>org.springframework.plugin</groupId>
    <artifactId>spring-plugin-core</artifactId>
    <version>2.0.0.RELEASE</version>
</dependency>
<!-- https://mvnrepository.com/artifact/io.springfox/springfox-swagger2 -->
<dependency>
   <groupId>io.springfox</groupId>
   <artifactId>springfox-swagger2</artifactId>
   <version>2.9.2</version>
</dependency>
<!-- https://mvnrepository.com/artifact/io.springfox/springfox-swagger-ui -->
<dependency>
   <groupId>io.springfox</groupId>
   <artifactId>springfox-swagger-ui</artifactId>
   <version>2.9.2</version>
</dependency>
```

编写配置类

```java
package com.rainsun.yuqing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@Configuration // 标明是配置类
@EnableSwagger2 //开启swagger功能
public class SwaggerConfig {
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo()) // 用于生成API信息
                .select() // select()函数返回一个ApiSelectorBuilder实例,用来控制接口被swagger做成文档
                .apis(RequestHandlerSelectors.basePackage("com.rainsun.yuqing.controller"))
                //.withClassAnnotation(RestController.class) // 扫描带有指定注解的类下所有接口
                //.withMethodAnnotation(PostMapping.class) // 扫描带有只当注解的方法接口
                //.apis(RequestHandlerSelectors.any()) // 扫描所有

                // 选择所有的API,如果你想只为部分API生成文档，可以配置这里
                .paths(PathSelectors.any()// any() // 满足条件的路径，该断言总为true
                        //.none() // 不满足条件的路径，该断言总为false（可用于生成环境屏蔽 swagger）
                        //.ant("/user/**") // 满足字符串表达式路径
                        //.regex("") // 符合正则的路径
                )
                .build();
    }

    /**
     * 用于定义API主界面的信息，比如可以声明所有的API的总标题、描述、版本
     * @return
     */
    private ApiInfo apiInfo() {

        Contact contact = new Contact(
                "YuqingXiong", // 作者姓名
                "https://blog.csdn.net/", // 作者网址
                "rainsun@xxx.com"); // 作者邮箱

        return new ApiInfoBuilder()
                .title("MatchProject项目API") //  可以用来自定义API的主标题
                .description("MatchProject项目SwaggerAPI管理") // 可以用来描述整体的API
                .termsOfServiceUrl("https://github.com/YuqingXiong") // 用于定义服务的域名（跳转链接）
                .version("1.0") // 可以用来定义版本
                .license("Swagger-的使用")
                .licenseUrl("https://blog.csdn.net")
                .contact(contact)
                .build();
    }
}
```

springboot3.0和swagger2+不兼容

## knif4j

[Spring Boot3整合knife4j(swagger3)_springboot3 knife4j-CSDN博客](https://blog.csdn.net/qq_62262918/article/details/135761392)

```xml
 <dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    <version>4.4.0</version>
</dependency>
```

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

# springdoc-openapi项目配置
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  api-docs:
    path: /v3/api-docs
  group-configs:
    - group: 'default'
      paths-to-match: '/**'
      packages-to-scan: com.rainsun.yuqing
# knife4j的增强配置，不需要增强可以不配
knife4j:
  enable: true
  setting:
    language: zh_cn
```

启动后访问：

对于swagger

- 访问：`http://server:port/context-path/swagger-ui.html`

- 实际上访问的就是：`http://localhost:8080/api/swagger-ui/index.html`

对于 Knife4j（默认doc.html）

- 实际访问：`http://localhost:8080/api/doc.html`

> 注意：要加上前缀 /api 

![image-20240409163412230](https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404091634427.png)

## 防止接口文档暴露

当前yaml配置文件在 dev 环境下生效：

<img src="https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404092155505.png" alt="image-20240409215548421" style="zoom:50%;" />

设置Profile注解，使得接口文档只在dev环境下才注入bean。开发环境下不注入则无法访问接口文档

```java
@Configuration
@Profile("dev")
public class Knife4jConfig {
    @Bean
    public OpenAPI springShopOpenApi() {
        return new OpenAPI()
                // 接口文档标题
                .info(new Info().title("MatchProject")
                // 接口文档简介
                .description("这是基于Knife4j OpenApi3的测试接口文档")
                // 接口文档版本
                .version("1.0版本")
                // 开发者联系方式
                .contact(new Contact().name("rainsun")
                        .email("000000000@qq.com")));

    }
}
```



# 数据导入

1. 爬虫
2. Excel 导入

## 读取 Excel 数据到内存 

使用 easyexcel 库导入 excel 数据：[必读 | Easy Excel (alibaba.com)](https://easyexcel.opensource.alibaba.com/qa/)

### 准备：

定义对象的每个属性与Excel表中每一列的对应关系

```java
package com.rainsun.yuqing.once;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class FriendTableUserInfo {
    @ExcelProperty("成员编号")
    private String planetCode;
    @ExcelProperty("成员昵称")
    private String username;
}
```

### 读取模式1：监听器

监听器定义：

```java
package com.rainsun.yuqing.once;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;

// 有个很重要的点 DemoDataListener 不能被spring管理，要每次读取excel都要new,然后里面用到spring可以构造方法传进去
@Slf4j
public class TableListener implements ReadListener<FriendTableUserInfo> {

    /**
     * 这个每一条数据解析都会来调用
     *
     * @param data    one row value. Is is same as {@link AnalysisContext#readRowHolder()}
     * @param context
     */
    @Override
    public void invoke(FriendTableUserInfo data, AnalysisContext context) {
        System.out.println(data);
    }

    /**
     * 所有数据解析完成了 都会来调用
     *
     * @param context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        System.out.println("已解析完成");
    }
}
```

### 读取模式2：同步读

不创建监听器，一次读取完整的数据。

操作简单，但一次读的数据量太大会造成等待时间过长，而且一次性读出的数据都会存在内存中可能造成内存溢出

### 测试：

```java
package com.rainsun.yuqing.once;

import com.alibaba.excel.EasyExcel;
import java.util.List;


public class ImportExcel {
    public static void main(String[] args) {
        String fileName = "D:\\CodeProject\\Java\\MatchProject\\rainsun-backend\\src\\main\\resources\\testExcel.xlsx";
//        readByListener(fileName);
        synchronousRead(fileName);
    }

    /**
     * 1. 监听器读，每次读一行就会调用监听器
     * @param fileName
     */
    public static void readByListener(String fileName){
        EasyExcel
                .read(fileName, FriendTableUserInfo.class, new TableListener())
                .sheet().doRead();
    }


    /**
     * 2. 同步的返回 如果数据量大会把数据放到内存里面
     */
    public static void synchronousRead(String fileName) {
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 同步读取会自动finish
        List<FriendTableUserInfo> totalList = EasyExcel.read(fileName).head(FriendTableUserInfo.class).sheet().doReadSync();
        for(FriendTableUserInfo userInfo : totalList){
            System.out.println(userInfo);
        }
    }
}
```

## 插入新用户

1,xiongyuqing,sun123,https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202401231713002.jpg,0,xxx,123,123,0,2024-01-22 17:12:34,2024-01-26 14:15:12,0,1,,"[""java"", ""c++"", ""python""]",
2,testRain,testRain,https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202401231713002.jpg,0,77f3939521f9f8df6dbe4430211e2b2c,123,456,0,2024-01-23 10:25:05,2024-04-07 15:30:43,0,0,2,"[""java"", ""c++"", ""python""]",
3,xiongyuqing,rainsun,https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202401231713002.jpg,0,77f3939521f9f8df6dbe4430211e2b2c,123,456,0,2024-01-23 10:25:19,2024-01-24 17:09:31,0,1,3,"[""男"", ""java""]",
4,rain,rain,https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202401231713002.jpg,1,77f3939521f9f8df6dbe4430211e2b2c,123,456,0,2024-01-24 15:59:46,2024-01-25 10:46:45,0,1,1,"[""女"", ""java""]",
7,testRain,rain123,https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202401231713002.jpg,0,xxx,123,456,0,2024-01-26 16:22:50,2024-01-26 16:22:50,0,0,4,"[""男"", ""python""]",""
8,testRain,xyq123,https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202401231713002.jpg,0,xxx,123,456,0,2024-01-26 16:30:51,2024-01-26 16:30:51,0,0,5,"[""女"", ""python""]",

### 编写一次性任务

for 循环插入数据的问题：

1. 建立和释放数据库链接（批量查询解决）
2. for 循环是绝对线性的，依次插入的。（多线程插入）

#### 单线程执行

xxxApplication类上使用 `@EnableScheduling` 再给某个函数加上`@Schedued(xxxx,xx)` 注解，可以定时执行该函数

这里暂时在Test里执行：

```java
package com.rainsun.yuqing.service;

import com.rainsun.yuqing.mapper.UserMapper;
import com.rainsun.yuqing.model.domain.User;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

@SpringBootTest
public class InsertUserTest {
    @Resource
    private UserMapper userMapper;

    /**
     * 批量插入用户
     */
    @Test
    public void doInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 1000;
        for(int i = 0; i < INSERT_NUM; ++i){
            User user = new User();
            user.setUsername("朋友用户");
            user.setUserAccount("friendUser");
            user.setAvatarUrl("https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202401231713002.jpg");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("12345678910");
            user.setEmail("123@qq.com");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("1111111");
            user.setTags("[]");
            userMapper.insert(user);
        }
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
```

但是这样太慢了。

可以看到 mysql 在插入数据的时候，需要建立一个 SqlSession ，插入数据后，再Closing 这个Session。

![image-20240418224545571](https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404182245666.png)

每一条数据都需要这么建立连接又断开就非常慢，所以这里 mybatis 提供了批量插入的接口，建立一个session，批量插入多个数据：

```java
package com.rainsun.yuqing.service;

import com.rainsun.yuqing.mapper.UserMapper;
import com.rainsun.yuqing.model.domain.User;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class InsertUserTest {
    @Resource
    private UserService userService;

    /**
     * 批量插入用户
     */
    @Test
    public void doInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 1000;
        List<User> userList = new ArrayList<>();
        for(int i = 0; i < INSERT_NUM; ++i){
            User user = new User();
            user.setUsername("朋友用户");
            user.setUserAccount("friendUser");
            user.setAvatarUrl("https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202401231713002.jpg");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("12345678910");
            user.setEmail("123@qq.com");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("1111111");
            user.setTags("[]");
            userList.add(user);
        }
        userService.saveBatch(userList, 100);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
```

![image-20240418225119248](https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202404182251329.png)

#### 多线程并发

CompletableFuture 定义一个任务数组，每个插入的分组是一个任务，将任务数组交给多个线程执行

线程池不定义的话采用默认线程池，线程数是CPU核数

```java
package com.rainsun.yuqing.service;

import com.rainsun.yuqing.mapper.UserMapper;
import com.rainsun.yuqing.model.domain.User;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SpringBootTest
public class InsertUserTest {
    @Resource
    private UserService userService;

    /**
     * 批量插入用户
     */
    @Test
    public void doInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 1000;
        List<User> userList = new ArrayList<>();
        for(int i = 0; i < INSERT_NUM; ++i){
            User user = new User();
            user.setUsername("朋友用户");
            user.setUserAccount("friendUser");
            user.setAvatarUrl("https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202401231713002.jpg");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("12345678910");
            user.setEmail("123@qq.com");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("1111111");
            user.setTags("[]");
            userList.add(user);
        }
        userService.saveBatch(userList, 100);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

    /**
     * 并发批量插入用户
     */
    @Test
    public void doConcurrencyInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 1000;
        final int INSERT_GROUP_NUM = 100;
        int j = 0;
        // 任务数组
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for(int i = 0; i < 10; ++ i){
            List<User> userList = new ArrayList<>();
            do {
                ++j;
                User user = new User();
                user.setUsername("朋友用户");
                user.setUserAccount("friendUser");
                user.setAvatarUrl("https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202401231713002.jpg");
                user.setGender(0);
                user.setUserPassword("12345678");
                user.setPhone("12345678910");
                user.setEmail("123@qq.com");
                user.setUserStatus(0);
                user.setUserRole(0);
                user.setPlanetCode("1111111");
                user.setTags("[]");
                userList.add(user);
            }while(j % 100 != 0);
            // 异步执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                userService.saveBatch(userList, INSERT_GROUP_NUM);
            });
            // 加入到任务数组
            futureList.add(future);
        }
        // 等待所有异步任务完成
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
```

##### 自定义线程池

设置线程池的核心线程数大小，最大数量，等待时间，等待队列长度，（拒绝策略）

根据任务类型设置线程池的核心线程数：

- CPU密集型：分配核心线程数 = CPU - 1
- IO 密集型：分配核心线程数 大于 CPU 核数

```java
package com.rainsun.yuqing.service;

import com.rainsun.yuqing.mapper.UserMapper;
import com.rainsun.yuqing.model.domain.User;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
public class InsertUserTest {
    @Resource
    private UserService userService;

    private ExecutorService executorService = new ThreadPoolExecutor(60, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    /**
     * 批量插入用户
     */
    @Test
    public void doInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 1000;
        List<User> userList = new ArrayList<>();
        for(int i = 0; i < INSERT_NUM; ++i){
            User user = new User();
            user.setUsername("朋友用户");
            user.setUserAccount("friendUser");
            user.setAvatarUrl("https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202401231713002.jpg");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("12345678910");
            user.setEmail("123@qq.com");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("1111111");
            user.setTags("[]");
            userList.add(user);
        }
        userService.saveBatch(userList, 100);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

    /**
     * 并发批量插入用户
     */
    @Test
    public void doConcurrencyInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 10000;
        final int BATCH_SIZE = 1000;
        int j = 0;
        // 任务数组
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for(int i = 0; i < 20; ++ i){
            List<User> userList = new ArrayList<>();
            do {
                ++j;
                User user = new User();
                user.setUsername("朋友用户");
                user.setUserAccount("friendUser");
                user.setAvatarUrl("https://xiongyuqing-img.oss-cn-qingdao.aliyuncs.com/img/202401231713002.jpg");
                user.setGender(0);
                user.setUserPassword("12345678");
                user.setPhone("12345678910");
                user.setEmail("123@qq.com");
                user.setUserStatus(0);
                user.setUserRole(0);
                user.setPlanetCode("1111111");
                user.setTags("[]");
                userList.add(user);
            }while(j % BATCH_SIZE != 0);
            // 异步执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("threadName: " + Thread.currentThread().getName());
                userService.saveBatch(userList, BATCH_SIZE);
            }, executorService);
            // 加入到任务数组
            futureList.add(future);
        }
        // 等待所有异步任务完成
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
```



