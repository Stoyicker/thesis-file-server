package com.jorge.thesis.services;

import com.jorge.thesis.data.MessageManagerSingleton;
import com.jorge.thesis.io.net.HTTPRequestsSingleton;
import com.jorge.thesis.util.ConfigVars;
import com.squareup.okhttp.Request;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class MessagesService extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        synchronized (this) {
            final String messageId = req.getParameter("messageid"), bodyType = req.getParameter("type"), fileName,
                    tagsFileName = ConfigVars.MESSAGE_TAGS_FILE_NAME;

            if (messageId == null || bodyType == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            switch (bodyType.toLowerCase(Locale.ENGLISH)) {
                case "normal":
                    fileName = ConfigVars.MESSAGE_BODY_FILE_NAME;
                    break;
                case "sketchboard":
                    fileName = ConfigVars.MESSAGE_SKETCHBOARD_FILE_NAME;
                    if (Files.exists(Paths.get(fileName))) {
                        resp.addHeader("Message-Identifier", "string; identifier=" + messageId);
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    break;
                default:
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
            }

            resp.addHeader("Content-Disposition", "attachment; filename=" + fileName);

            final File bodyFile = Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId, fileName).toFile(), tagsFile =
                    Paths
                            .get(ConfigVars.MESSAGE_CONTAINER, messageId, tagsFileName).toFile();

            if (!bodyFile.exists()) {
                resp.addHeader("Message-Identifier", "string; identifier=" + messageId);
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            final JSONObject object = new JSONObject();

            try {
                object.put("status", "ok");
                if (!tagsFile.exists())
                    object.put("tags", "");
                else
                    object.put("tags", FileUtils.readFileToString(tagsFile, ConfigVars.SERVER_CHARSET));
                object.put("content_html", FileUtils.readFileToString(bodyFile));
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

            final String content_html;
            final List<String> cleanTags = new LinkedList<>();
            final Pattern tagFormatPattern = Pattern.compile("[a-z0-9_]+");

            try {
                final JSONObject object = new JSONObject(IOUtils.toString(req.getReader())); //Data from the body as
                // characters
                // (fine
                // for JSON)
                content_html = object.getString("content_html");
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
                if (MessageManagerSingleton.getInstance().processMessage(content_html, cleanTags)) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    final StringBuilder cleanTagsTogether = new StringBuilder();
                    for (Iterator<String> it = cleanTags.iterator(); it.hasNext(); ) {
                        cleanTagsTogether.append(it.next());
                        if (it.hasNext())
                            cleanTagsTogether.append(TagService.TAG_SEPARATOR);
                    }
                    final String requestURL = ConfigVars.GCM_SERVER_ADDR.trim() + "/tags" + "?type=sync&tags=" +
                            cleanTagsTogether;
                    final Response gcmResp = HTTPRequestsSingleton.getInstance().performRequest(new Request.Builder()
                            .url(requestURL).post(null)
                            .build());
                    System.out.println(gcmResp.toString());
                } else
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
