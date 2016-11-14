package com.example.hanhnv.camapp;

import java.util.Calendar;

/**
 * Created by hanhnv on 21/10/2016.
 */

public class MMeasureTime {

    long begin;// = System.nanoTime();

    public MMeasureTime() {
        begin = System.nanoTime();
    }

    // Microsecond
    public int untilNow(){
        return (int)((System.nanoTime() - begin) / 1000);
    }

    public void update(){
        begin = System.nanoTime();
    }
}
