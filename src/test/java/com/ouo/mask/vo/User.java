package com.ouo.mask.vo;

import cn.hutool.core.text.StrBuilder;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ouo.mask.core.annotation.*;
import com.ouo.mask.core.enums.SensitiveTypeEnum;
import com.ouo.mask.jackson.CharSequenceSerializer;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "uid" // 指定标识字段
)
public class User extends BasicUser {

    private static final String ip = "192.168.55.13";
    private final String FNAME = "常量可序列化";
    @JsonSerialize(using = CharSequenceSerializer.class)
    public StrBuilder idCard;
    @Mask(type = SensitiveTypeEnum.ADDRESS)
    String[] addr;

    @Mask(type = SensitiveTypeEnum.FULL_NAME)
    String name;
    @Repl(posns = {@Repl.Posn(i = 3), @Repl.Posn(i = 8, rv = "#")})
    String extra;
    @Repl(posns = {@Repl.Posn(i = 3), @Repl.Posn(i = 8, rv = "#?12$%34")})
    //@Mask(type = SensitiveTypeEnum.FIXED_PHONE)
    String tel;
    @Mask(type = SensitiveTypeEnum.MOBILE_PHONE/*, custom = Mask.CommonMaskOptions.PRE_3_SUF_3*/)
    String phone;
    @Mask(type = SensitiveTypeEnum.MOBILE_PHONE, show = @Mask.CustomShow(pre = 2, suf = 3))
    String mobile;
    @Mask(type = SensitiveTypeEnum.BANK_CARD)
    StringBuilder bankCard; //StrBuilder
    @Mask(type = SensitiveTypeEnum.NUMBER)
    String amount;
    @Mask(type = SensitiveTypeEnum.CAR_LICENSE)
    String car;
    @Mask(type = SensitiveTypeEnum.PASSPORT)
    String passport;
    @Regex(pattern = "(\\d{4})年(\\d{1,2})月(\\d{1,2})日.*", rv = "$1年**月**日")
    String date;
    @Empty
    Attach attach;
    private transient String tname = "transient变量不可序列化";
    private int[] ages;

    @JsonIdentityReference(alwaysAsId = true)
    private Map map;
    private User user; // 自引用

    public User() {

    }

    public User(String str) {
    }

    public void setName(String name) {
        this.name = name;
    }

    @Setter
    @Getter
    public class Attach {
        //@Empty
        @Mask(type = SensitiveTypeEnum.ADDRESS, show = @Mask.CustomShow(pre = 1, suf = 0))
        List<String> hobbies;
        //@Regex(pattern = "(\\w{3})\\w+(@qq.com)", rv = "$1***$2")
        @Mask(type = SensitiveTypeEnum.EMAIL)
        String email;
        @Hash(algorithm = Hash.AlgorithmEnum.SM3, salt = "ws@4q#")
        String card;
    }
}
