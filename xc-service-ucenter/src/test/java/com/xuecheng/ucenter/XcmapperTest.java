package com.xuecheng.ucenter;

import com.xuecheng.framework.domain.ucenter.XcMenu;
import com.xuecheng.ucenter.dao.XcMenuMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class XcmapperTest {
    @Autowired
    XcMenuMapper xcmenuMapper;
    @Test
    public void cmapperTest(){
        List<XcMenu> xcMenus = xcmenuMapper.selectPermissionByUserId("46");
        System.out.println(xcMenus);
    }
}
