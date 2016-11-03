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

    // Milisecond
    public int untilNow(){
        return (int)((System.nanoTime() - begin) / 1000000);
    }

    public void update(){
        begin = System.nanoTime();
    }
}
