package com.example.weather.sdk.enums;

public enum Mode {
    ON_DEMAND,
    POLLING;

    public static Mode toMode(String type){
        try{
            return Mode.valueOf(type.toUpperCase().trim());
        }catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Укажите ON_DEMAND или POLLING в любом регистре");
        }
    }
}
