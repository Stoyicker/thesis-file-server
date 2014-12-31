package com.jorge.thesis.io.files;

public class PurgerSingleton {

    private static final Object LOCK = new Object();
    private static volatile PurgerSingleton mInstance;

    public static PurgerSingleton getInstance() {
        PurgerSingleton ret = mInstance;
        if (ret == null) {
            synchronized (LOCK) {
                ret = mInstance;
                if (ret == null) {
                    ret = new PurgerSingleton();
                    mInstance = ret;
                }
            }
        }
        return ret;
    }
}
