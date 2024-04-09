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