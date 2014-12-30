package com.jorge.thesis.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

public final class ConfigVars {

    public static String PORT, MESSAGE_CONTAINER;
    private static Boolean INITIALIZED = Boolean.FALSE;

    private ConfigVars() throws IllegalAccessException {
        throw new IllegalAccessException("DO NOT CONSTRUCT " + ConfigVars.class.getName());
        //Forbid construction even through reflection
    }

    public static synchronized void init() {
        if (!INITIALIZED) {
            PORT = System.getenv("COMM_EXP_FILE_SERV_PORT");
            try {
                MESSAGE_CONTAINER = IOUtils.toString(ConfigVars.class.getResourceAsStream
                        ("/message_folder_name"));
                INITIALIZED = Boolean.TRUE;
            } catch (IOException e) {
                e.printStackTrace(System.err);
                //Should never happen
                throw new IllegalStateException("Error during initialisation of configuration.");
            }
        }
    }

}
