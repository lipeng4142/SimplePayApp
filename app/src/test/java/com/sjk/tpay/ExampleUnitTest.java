package com.sjk.tpay;

import com.sjk.simplepay.utils.StrEncode;

import org.junit.Test;

import java.text.DecimalFormat;


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
//        assertEquals(4, 2 + 2);
        Float k=1.04999f;
        k=(k*100);
        System.out.println( new DecimalFormat("#").format(k));
        System.out.println((k*100)+"");
        System.out.println(k.isInfinite()+"");
        String k2="1.05";
        System.out.println((int)(Float.valueOf(k2)*100));


        System.out.println(StrEncode.encoderByDES("bswv54dfsgdfsrtgt4egfgfgtygfg", "DXV83nmdfe3"));
    }


}