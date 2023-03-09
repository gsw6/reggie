package com;

import org.junit.jupiter.api.Test;

public class UpLoadFileTest {
    @Test
    public void test1(){
        String a="sdsdf1?dasd";
        String substring = a.substring(0,a.indexOf("?"));
        substring = substring.substring(substring.length() - 1);
        System.out.println(substring);
    }
}
