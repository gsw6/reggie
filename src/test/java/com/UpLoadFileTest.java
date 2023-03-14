package com;

import org.junit.jupiter.api.Test;

import java.util.List;

public class UpLoadFileTest {
    @Test
    public void test1(){
        String a="sdsdf1?dasd";
        a.toString();
        String substring = a.substring(0,a.indexOf("?"));
        substring = substring.substring(substring.length() - 1);
        System.out.println(substring);
    }
}
