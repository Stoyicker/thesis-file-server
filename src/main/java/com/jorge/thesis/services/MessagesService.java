package com.jorge.thesis.services;

import com.jorge.thesis.data.MessageManagerSingleton;
import com.jorge.thesis.io.net.HTTPRequestsSingleton;
import com.jorge.thesis.util.ConfigVars;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import java.util.*;
import java.util.regex.Pattern;

public final class MessagesService extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        synchronized (this) {
            final String messageId = req.getParameter("messageid"),
                    tagsFileName = ConfigVars.MESSAGE_TAGS_FILE_NAME;

            if (messageId == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            final File normalFile = Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId, ConfigVars
                    .MESSAGE_BODY_FILE_NAME).toFile(),
                    sketchBoardFile = Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId, ConfigVars
                            .MESSAGE_SKETCHBOARD_FILE_NAME).toFile(),
                    tagsFile =
                            Paths
                                    .get(ConfigVars.MESSAGE_CONTAINER, messageId, tagsFileName).toFile();

            final String[] allFilesInFolder = Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId).toFile().list();
            List<String> allAttachments = new LinkedList<>();
            for (String x : allFilesInFolder) {
                if (!x.contentEquals(ConfigVars.MESSAGE_TIMESTAMP_FILE_NAME) && !x.contentEquals(ConfigVars
                        .MESSAGE_BODY_FILE_NAME) && !x.contentEquals(ConfigVars.MESSAGE_SKETCHBOARD_FILE_NAME) && !x
                        .contentEquals(ConfigVars.MESSAGE_TAGS_FILE_NAME))
                    allAttachments.add(x);
            }

            final String sender = FileUtils.readFileToString(Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId,
                    ConfigVars.MESSAGE_SENDER_FILE_NAME).toFile());

            final JSONObject object = new JSONObject();

            try {
                object.put("sender", sender);
                object.put("status", "ok");
                if (!tagsFile.exists())
                    object.put("tags", Collections.<String>emptyList());
                else {
                    final List<String> nonEmptyTags = new LinkedList<>();
                    final List<String> rawTags = FileUtils.readLines(tagsFile, ConfigVars.SERVER_CHARSET);
                    for (String x : rawTags) {
                        final String cleanX = x.toLowerCase(Locale.ENGLISH).trim();
                        if (!cleanX.isEmpty() && !cleanX.contentEquals("\n") && !nonEmptyTags.contains(cleanX)) {
                            nonEmptyTags.add(cleanX);
                        }
                    }
                    object.put("tags", nonEmptyTags);
                }
                Boolean exists;
                exists = normalFile.exists();
                object.put("has_Normal", exists);
                if (exists) {
                    object.put("content_html", FileUtils.readFileToString(normalFile));
                }
                exists = sketchBoardFile.exists();
                object.put("has_SketchBoard", exists);
                if (exists) {
                    object.put("sketchboard_content_html", FileUtils.readFileToString(sketchBoardFile));
                }
                JSONArray attachmentNames = new JSONArray();
                allAttachments.forEach(attachmentNames::put);
                object.put("has_attachments", attachmentNames.length() > 0);
                object.put("attachments", attachmentNames);
                resp.setContentType("application/json");
                resp.getWriter().print(object.toString());
                resp.setStatus(HttpServletResponse.SC_OK);
            } catch (JSONException e) {
                e.printStackTrace(System.err);
                //Should never happen
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        synchronized (this) {
            final String objContentType = req.getContentType();

            if (objContentType == null || !(objContentType.toLowerCase(Locale.ENGLISH).trim().contentEquals
                    ("application/json") || !objContentType.toLowerCase(Locale.ENGLISH).trim().contentEquals
                    ("application/json; charset=UTF-8"))) {
                System.err.println("Unexpected content-type header " + objContentType);
                resp.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
                return;
            }

            final String content_html, sender;
            final List<String> cleanTags = new LinkedList<>();
            final Pattern tagFormatPattern = Pattern.compile("[a-z0-9_]+");

            try {
                final JSONObject object = new JSONObject(IOUtils.toString(req.getReader())); //Data from the body as
                // characters
                // (fine
                // for JSON)
                content_html = object.getString("content_html");
                sender = object.getString("sender");
                JSONArray tagsAsJSONArray = object.getJSONArray("tags");
                for (Integer i = 0; i < tagsAsJSONArray.length(); i++) {
                    final String candidateTag = tagsAsJSONArray.getString(i).trim().toLowerCase(Locale.ENGLISH);
                    if (tagFormatPattern.matcher(candidateTag).matches() && !cleanTags.contains(candidateTag))
                        cleanTags.add(candidateTag);
                }
            } catch (JSONException e) {
                e.printStackTrace(System.err);
                //Should never happen
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (MessageManagerSingleton.getInstance().areMoreMessagesAllowed()) {
                final Integer messageId;
                if ((messageId = MessageManagerSingleton.getInstance().processMessage(sender, content_html,
                        cleanTags)) != -1) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    try {
                        final JSONObject obj = new JSONObject();
                        obj.put("status", "ok");
                        obj.put("msgid", messageId);
                        resp.getWriter().print(obj.toString());
                    } catch (JSONException e) {
                        e.printStackTrace(System.err);
                        //Should never happen
                    }
                    final StringBuilder cleanTagsTogether = new StringBuilder();
                    for (Iterator<String> it = cleanTags.iterator(); it.hasNext(); ) {
                        cleanTagsTogether.append(it.next());
                        if (it.hasNext())
                            cleanTagsTogether.append(TagService.TAG_SEPARATOR);
                    }
                    final String requestURL = ConfigVars.GCM_SERVER_ADDR.trim() + "/tags" + "?type=sync&tags=" +
                            cleanTagsTogether;
                    final Response gcmResp = HTTPRequestsSingleton.getInstance().performRequest(new Request.Builder()
                            .url(requestURL).post(RequestBody.create(MediaType.parse("text/plain"), ""))
                            .build());
                    System.out.println(gcmResp.toString());
                } else
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
