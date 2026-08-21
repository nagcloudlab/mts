package com.mts.p1;


class bar{
    public void m() {
        System.out.println("Bar.m()");
    }
}
public class Foo {

    public void m() {
        bar b = null;
        b.m();
        int v=12;
    }
    
}
