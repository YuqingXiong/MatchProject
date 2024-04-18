package com.rainsun.yuqing.once;
import java.util.Date;

import com.rainsun.yuqing.mapper.UserMapper;
import com.rainsun.yuqing.model.domain.User;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Component
public class InsertUsers {
    @Resource
    private UserMapper userMapper;

    /**
     * 批量插入用户
     */
//    @Scheduled(fix...
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
