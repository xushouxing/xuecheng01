package com.xuecheng.manage_course.service;
import com.xuecheng.framework.domain.course.ext.CategoryNode;
import com.xuecheng.manage_course.dao.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    /**
     * 查询分类
     * @return
     */
    public CategoryNode findList() {
        return categoryMapper.findList();
    }
}
