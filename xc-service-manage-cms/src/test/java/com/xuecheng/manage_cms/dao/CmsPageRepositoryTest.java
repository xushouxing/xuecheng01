package com.xuecheng.manage_cms.dao;
import com.xuecheng.framework.domain.cms.CmsPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CmsPageRepositoryTest {
    @Autowired
    private CmsPageRepository cmsPageRepository;

    /**
     * 查询条件如下：
     * 站点Id：精确匹配  SiteId
     * 模板Id：精确匹配   TemplateId
     * 页面别名：模糊匹配   pageAliase
     */
    @Test
    public void queryCmsPageByExample(){
        //参数一 example
             //条件值
        CmsPage  cmsPage=new CmsPage();
        cmsPage.setPageAliase("播");
             //条件匹配器
        ExampleMatcher exampleMatcher=ExampleMatcher.matching();
             //模糊查询
        exampleMatcher=exampleMatcher.withMatcher("pageAliase",ExampleMatcher.GenericPropertyMatchers.contains());
        Example<CmsPage> example=Example.of(cmsPage,exampleMatcher);
        //参数二
        int page=0;
        int size=10;
        PageRequest pageRequest = PageRequest.of(page, size);
        //查询
        Page<CmsPage> repositoryAll = cmsPageRepository.findAll(example, pageRequest);
        List<CmsPage> content = repositoryAll.getContent();
    }
}
