package com.jorge.thesis.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;

public final class ConfigVars {

    private static Boolean INITIALIZED = Boolean.FALSE;

    public static String MESSAGE_BODY_FILE_NAME;
    public static String PORT, MESSAGE_CONTAINER;
    public static String MESSAGE_TAGS_FILE_NAME;
    public static Charset SERVER_CHARSET;

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
                MESSAGE_BODY_FILE_NAME = IOUtils.toString(ConfigVars.class.getResourceAsStream
                        ("/message_body_file_name"));
                MESSAGE_TAGS_FILE_NAME = IOUtils.toString(ConfigVars.class.getResourceAsStream
                        ("/message_tags_file_name"));
                SERVER_CHARSET = Charset.forName(IOUtils.toString(ConfigVars.class.getResourceAsStream
                        ("/charset")));
                INITIALIZED = Boolean.TRUE;
            } catch (IOException e) {
                e.printStackTrace(System.err);
                //Should never happen
                throw new IllegalStateException("Error during initialisation of configuration.");
            }
        }
    }

}
