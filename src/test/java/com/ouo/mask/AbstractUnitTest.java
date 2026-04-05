package com.ouo.mask;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/***********************************************************
 * 单元测试基类
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
//Junit4需要@RunWith(SpringRunner.class)+@SpringBootTest配置合；而Junit5不需要@RunWith
//@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DesensitizationAutoConfiguration.class}
/*, properties = {"classpath*:application.yml"}*/)
@EnableAutoConfiguration
public abstract class AbstractUnitTest {
}
