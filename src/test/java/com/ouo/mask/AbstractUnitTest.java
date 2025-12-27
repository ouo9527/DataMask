package com.ouo.mask;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

//Junit4需要@RunWith(SpringRunner.class)+@SpringBootTest配置合；而Junit5不需要@RunWith
//@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DesensitizationAutoConfiguration.class}
/*, properties = {"classpath*:application.yml"}*/)
@EnableAutoConfiguration
public abstract class AbstractUnitTest {
}
