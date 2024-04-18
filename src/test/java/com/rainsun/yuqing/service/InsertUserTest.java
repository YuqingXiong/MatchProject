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
