package com.jorge.thesis.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;

public final class ConfigVars {

    public static String MESSAGE_BODY_FILE_NAME;
    public static String PORT, MESSAGE_CONTAINER;
    public static String MESSAGE_TAGS_FILE_NAME;
    public static String MESSAGE_SKETCHBOARD_FILE_NAME;
    public static String MESSAGE_TIMESTAMP_FILE_NAME;
    public static String GCM_SERVER_ADDR;
    public static Charset SERVER_CHARSET;
    private static String CONFIGURATION_FOLDER_NAME;
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
                MESSAGE_BODY_FILE_NAME = IOUtils.toString(ConfigVars.class.getResourceAsStream
                        ("/message_body_file_name"));
                MESSAGE_TAGS_FILE_NAME = IOUtils.toString(ConfigVars.class.getResourceAsStream
                        ("/message_tags_file_name"));
                SERVER_CHARSET = Charset.forName(IOUtils.toString(ConfigVars.class.getResourceAsStream
                        ("/charset")));
                MESSAGE_SKETCHBOARD_FILE_NAME = IOUtils.toString(ConfigVars.class.getResourceAsStream
                        ("/message_sketchboard_file_name"));
                MESSAGE_TIMESTAMP_FILE_NAME = IOUtils.toString(ConfigVars.class.getResourceAsStream
                        ("/message_timestamp_file_name"));
                CONFIGURATION_FOLDER_NAME = IOUtils.toString(ConfigVars.class.getResourceAsStream
                        ("/configuration_folder_name"));
                GCM_SERVER_ADDR = FileUtils.readFileToString(Paths.get(CONFIGURATION_FOLDER_NAME, "addr" +
                        ".conf").toFile());
                if (GCM_SERVER_ADDR == null)
                    throw new IllegalStateException("GCM_SERVER_ADDR not provided.");
                INITIALIZED = Boolean.TRUE;
            } catch (IOException e) {
                e.printStackTrace(System.err);
                //Should never happen
                throw new IllegalStateException("Error during initialisation of configuration.");
            }
        }
    }

}
