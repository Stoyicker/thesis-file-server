package com.jorge.thesis.datamodel;

import com.jorge.thesis.util.ConfigVars;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class MessageManagerSingleton {

    private static final Object LOCK = new Object();
    private static volatile MessageManagerSingleton mInstance;
    private String mLastUsedId = null;

    public static MessageManagerSingleton getInstance() {
        MessageManagerSingleton ret = mInstance;
        if (ret == null) {
            synchronized (LOCK) {
                ret = mInstance;
                if (ret == null) {
                    ret = new MessageManagerSingleton();
                    mInstance = ret;
                }
            }
        }
        return ret;
    }

    public synchronized String generateMessageId() {
        if (mLastUsedId == null) {
            Integer possibleId = new File(ConfigVars.MESSAGE_CONTAINER).list().length;

            while (Files.exists(Paths.get(ConfigVars.MESSAGE_CONTAINER, possibleId.toString()))) {
                possibleId++;
            }

            mLastUsedId = possibleId.toString();
        } else
            while (Files.exists(Paths.get(ConfigVars.MESSAGE_CONTAINER, mLastUsedId))) {
                try {
                    Integer t = Integer.valueOf(mLastUsedId) + 1;
                    mLastUsedId = t.toString();
                    if (t == Integer.MAX_VALUE) {
                        System.out.println("IT IS CRITICALLY IMPORTANT THAT YOU SCHEDULE A PURGE AND RESTART THE " +
                                "SERVER.");
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace(System.err);
                    //Should never happen
                    throw new IllegalStateException("Messages id are not being properly generated. Found " +
                            mLastUsedId);
                }
            }

        return mLastUsedId;
    }

    public synchronized Boolean areMoreMessagesAllowed() {
        return new File(ConfigVars.MESSAGE_CONTAINER).list().length < Integer.MAX_VALUE;
    }

    public synchronized Boolean processMessage(String content_html, List<String> tags) {
        //TODO processMessage
        return Boolean.FALSE;
    }
}
