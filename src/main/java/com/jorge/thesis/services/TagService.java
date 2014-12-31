package com.jorge.thesis.services;

import com.jorge.thesis.util.ConfigVars;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public final class TagService extends HttpServlet {

    static final String TAG_SEPARATOR = "-";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        synchronized (this) {
            final String epoch = req.getParameter("epoch"), allTags = req.getParameter("tags");

            if (epoch == null || allTags == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            final List<String> cleanTags = new LinkedList<>();
            final StringTokenizer tagsTokenizer = new StringTokenizer(allTags, TAG_SEPARATOR);
            final Pattern tagFormatPattern = Pattern.compile("[a-z0-9_]+");
            while (tagsTokenizer.hasMoreTokens()) {
                String cleanTag = tagsTokenizer.nextToken().trim().toLowerCase(Locale.ENGLISH);
                if (tagFormatPattern.matcher(cleanTag).matches() && !cleanTags.contains(cleanTag))
                    cleanTags.add(cleanTag);
            }

            String[] allMessages = Paths.get(ConfigVars.MESSAGE_CONTAINER).toFile().list();

            final List<String> messageIdsThatMatch = new LinkedList<>();
            final Long requestedEpochLimit = Long.parseLong(epoch);
            for (String messageId : allMessages) {
                if (!Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId).toFile().isDirectory()) {
                    //This should never happen but, just in case, don't touch
                    continue;
                }

                final File tagsFile = Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId, ConfigVars
                        .MESSAGE_TAGS_FILE_NAME).toFile(),
                        epochFile = Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId, ConfigVars
                                .MESSAGE_TIMESTAMP_FILE_NAME).toFile();

                if (Long.parseLong(FileUtils.readFileToString(epochFile, ConfigVars.SERVER_CHARSET)) >
                        requestedEpochLimit && fileContainsOneOrMoreTags(tagsFile, cleanTags)) {
                    messageIdsThatMatch.add(messageId);
                }
            }

            final JSONObject object = new JSONObject();
            try {
                object.put("status", "ok");
                final JSONArray array = new JSONArray();
                for (String messageId : messageIdsThatMatch) {
                    final JSONObject thisMsg = new JSONObject();
                    thisMsg.put("msgId", messageId);
                }
                object.put("messages", array);
                resp.getWriter().print(object.toString());
                resp.setContentType("application/json");
                resp.setStatus(HttpServletResponse.SC_OK);
            } catch (JSONException e) {
                e.printStackTrace(System.err);
                //Should never happen
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private synchronized Boolean fileContainsOneOrMoreTags(File tagsFile, List<String> cleanTags) throws IOException {
        List<String> lines = FileUtils.readLines(tagsFile, ConfigVars.SERVER_CHARSET);
        for (String line : lines)
            if (cleanTags.contains(line)) {
                return Boolean.TRUE;
            }
        return Boolean.FALSE;
    }
}
