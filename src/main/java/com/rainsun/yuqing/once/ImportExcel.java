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
     * 监听器读，每次读一行就会调用监听器
     * @param fileName
     */
    public static void readByListener(String fileName){
        EasyExcel
                .read(fileName, FriendTableUserInfo.class, new TableListener())
                .sheet().doRead();
    }


    /**
     * 同步的返回 如果数据量大会把数据放到内存里面
     */
    public static void synchronousRead(String fileName) {
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 同步读取会自动finish
        List<FriendTableUserInfo> totalList = EasyExcel.read(fileName).head(FriendTableUserInfo.class).sheet().doReadSync();
        for(FriendTableUserInfo userInfo : totalList){
            System.out.println(userInfo);
        }
    }
}
