package com.jorge.thesis.data;

import com.jorge.thesis.util.ConfigVars;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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

    private synchronized String generateMessageId() {
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
        final String messageId = generateMessageId();

        if (!Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId).toFile().mkdirs()) {
            System.err.println("Error when processing message " + messageId + " (folder creation). Aborting message " +
                    "processing.");
            //Should never happen
            return Boolean.FALSE;
        }

        try {
            FileUtils.writeStringToFile(Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId, ConfigVars
                            .MESSAGE_BODY_FILE_NAME).toAbsolutePath().toFile(), content_html, ConfigVars.SERVER_CHARSET,
                    Boolean.FALSE);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            //Should never happen
            return Boolean.FALSE;
        }

        //Clean the tags
        final List<String> cleanTags = new LinkedList<>();
        final Pattern tagFormatPattern = Pattern.compile("[a-z0-9_]+");
        for (String t : tags) {
            final String cleanTag = t.trim().toLowerCase(Locale.ENGLISH);
            if (tagFormatPattern.matcher(cleanTag).matches() && !cleanTags.contains(cleanTag))
                cleanTags.add(cleanTag);
        }

        try {
            for (String ct : cleanTags) {
                FileUtils.writeStringToFile(Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId, ConfigVars
                                .MESSAGE_TAGS_FILE_NAME).toAbsolutePath().toFile(), ct + "\n", ConfigVars
                                .SERVER_CHARSET,
                        Boolean.TRUE);
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            //Should never happen
            return Boolean.FALSE;
        }

        try {
            FileUtils.writeStringToFile(Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId, ConfigVars
                    .MESSAGE_TIMESTAMP_FILE_NAME).toAbsolutePath().toFile(), Long.toString(System.currentTimeMillis()
            ), ConfigVars
                    .SERVER_CHARSET, Boolean.FALSE);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            //Should never happen
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }
}
