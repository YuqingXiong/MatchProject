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

2. 内存查询

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

3. 优化

- 内存查询可以多线程并发查询进行优化
- 根据数据量选择查询方法
- 数据库连接足够，内存空间足够，可以并发同时查询，谁先返回查询结果就用谁
- SQL 查询与内存查询相结合。先用SQL过滤部分tag









