package com.ouo.mask;

import com.ximpleware.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/***********************************************************
 * XML处理单元测试
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
public class XmlTest {

    private static void traverseNodes(VTDNav vn, int depth) throws NavException {

        // 3. 处理当前节点
        int tokenType = vn.getTokenType(vn.getCurrentIndex());
        switch (tokenType) {
            case VTDNav.TOKEN_STARTING_TAG:  // 元素节点
                System.out.println(depth + "节点: " + vn.toString(vn.getCurrentIndex()));
                int textIdx = vn.getText();
                if (-1 != textIdx) {
                    switch (vn.getTokenType(textIdx)) {
                        case VTDNav.TOKEN_CHARACTER_DATA: // 普通文本节点内容（<name>John Doe</name>）
                            // toNormalizedString：解析转义字符，但将所有连续空白字符（空格/制表符/换行）‌压缩为单个空格
                            // toNormalizedString2：解析转义字符并‌保留原始空白格式‌（包括换行和制表符，若字符串中有换行符，则输出时不会换行）
                            // toString：解析转义字符，但不保留原始空白‌（若字符串中有换行符，则输出时会换行）
                            // toRawString：不解析转义字符，也不保留原始空白
                            System.out.println(vn.toNormalizedString(vn.getCurrentIndex()) + "节点普通文本: " + vn.toNormalizedString(textIdx));
                            //System.out.println(vn.toNormalizedString(vn.getCurrentIndex()) + "节点普通文本: " + vn.toString(textIdx));
                            break;
                        case VTDNav.TOKEN_CDATA_VAL: // CDATA区块内容（<![CDATA[<html>content</html>]]）
                            System.out.println(vn.toNormalizedString(vn.getCurrentIndex()) + "节点CDATA文本: " + vn.toNormalizedString(textIdx));
                            break;
                        case VTDNav.TOKEN_PI_NAME: // 处理指令(‌P‌rocessing ‌I‌nstruction)名称（<?xml-stylesheet ...?>）
                            break;
                        case VTDNav.TOKEN_PI_VAL: // 处理指令值内容（href="style.css" type="text/css"）
                            break;
                        case VTDNav.TOKEN_DTD_VAL: // 文档类型定义(‌D‌ocument ‌T‌ype ‌D‌efinition)内部子集值（!ELEMENT note (...) >）
                            break;
                        case VTDNav.TOKEN_ATTR_NAME: // XML元素的‌属性名称（<book id="123"> 中的 id）
                            break;
                        case VTDNav.TOKEN_ATTR_VAL: // XML元素的‌属性值（<book id="123"> 中的 "123"）
                            break;
                        case VTDNav.TOKEN_COMMENT: // XML注释内容（<!-- comment text -->）
                            break;
                        case VTDNav.TOKEN_DEC_ATTR_NAME: // XML声明中的属性名（在<?xml ...?>中的version、encoding）
                            break;
                        case VTDNav.TOKEN_DEC_ATTR_VAL: // XML声明中的属性值（在<?xml version="1.0"?>中的"1.0"）
                            break;
                        case VTDNav.TOKEN_DOCUMENT: // 表示整个文档的根（非元素节点）
                            break;
                        case VTDNav.TOKEN_ATTR_NS: // XML命名空间声明（xmlns或xmlns:prefix）
                            break;
                    }
                }

                processAttributes(vn, depth);  // 处理属性
                break;
            case VTDNav.TOKEN_CHARACTER_DATA:  // 文本节点，VTD-XML 将文本/CDATA 视为‌元素节点的附属物‌，而非独立节点
                System.out.println(depth + "文本: " + vn.toNormalizedString(vn.getCurrentIndex()));
                break;
            /*case VTDNav.TOKEN_PI:  // 处理指令
                System.out.println(depth + "处理指令: " + vn.toString(vn.getCurrentIndex()));
                break;*/
            // 其他类型节点可根据需要扩展
            default:
                System.out.println(depth + tokenType + ": " + vn.toString(vn.getCurrentIndex()));
        }

        // 4. 递归遍历子节点
        if (vn.toElement(VTDNav.FIRST_CHILD)) {
            do {
                traverseNodes(vn, depth + 1);  // 深度优先遍历
            } while (vn.toElement(VTDNav.NEXT_SIBLING));  // 遍历同级节点
            vn.toElement(VTDNav.PARENT);  // 返回父节点
        }
    }

    // 5. 属性遍历方法
    private static void processAttributes(VTDNav vn, int depth) throws NavException {
        int attrCount = vn.getAttrCount();
        for (int i = 1; i <= attrCount; i++) {
            String attrName = vn.toNormalizedString(vn.getCurrentIndex() + i); // 属性名
            if (vn.hasAttr(attrName)) { // 检查是否存在属性
                int attrVal = vn.getAttrVal(attrName);     // 属性值

                System.out.println(depth + "  └─ 属性: "
                        + attrName + " = \""
                        + attrVal + "\"");
            }
        }
    }

    @Test
    public void vtdxml() throws ParseException, NavException, ModifyException, IOException, TranscodeException {
        String xmlStr = "<student> <text><![CDATA[<name>张三丰</name>]]></text> <phones><phone val=\"23\">\r\n17722657194</phone><phone>18822657194</phone></phones><class><val>&lt;name>数学&lt;/name></val></class></student>";
        xmlStr = "<text>爱我中华</text>";
        // VTD-XML 仅支持特定编码（UTF-8/16、ISO-8859）。若遇 GBK 文件需预先转码：
        //String utf8String = new String(gbkBytes, "UTF-8");

        //‌性能优化‌
        // 1、对大型 XML 启用 Location Cache (LC) 加速层级访问：
        //vg.parseFile("large.xml", true);
        // 2、避免频繁调用 vn.toString()，优先使用索引操作

        VTDGen vg = new VTDGen();
        vg.setDoc(xmlStr.getBytes());       // 加载字节数组
        vg.parse(true);           // 启用完整解析（含层级索引），生成VTD记录
        VTDNav vn = vg.getNav();  // 获取导航对象
        XMLModifier md = new XMLModifier(vn); // 创建XML修改器
        md.updateToken(2, "hello");
        md.output(System.out);

        int depth = vn.getCurrentDepth(); // 记录当前深度

        traverseNodes(vn, depth);
    }
}
