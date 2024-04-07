package com.rainsun.yuqing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rainsun.yuqing.common.ErrorCode;
import com.rainsun.yuqing.exception.BusinessException;
import com.rainsun.yuqing.mapper.UserMapper;
import com.rainsun.yuqing.model.domain.Tag;
import com.rainsun.yuqing.mapper.TagMapper;
import com.rainsun.yuqing.model.domain.User;
import com.rainsun.yuqing.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
* @author rainsun
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2024-04-07 14:20:45
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




