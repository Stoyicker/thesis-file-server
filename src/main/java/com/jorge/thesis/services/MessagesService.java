package com.jorge.thesis.services;

import com.jorge.thesis.datamodel.MessageManagerSingleton;
import com.jorge.thesis.util.ConfigVars;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.ajax.JSON;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public final class MessagesService extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        final String messageId = req.getParameter("messageid"), fileName = ConfigVars.MESSAGE_BODY_FILE_NAME,
                tagsFileName = ConfigVars.MESSAGE_TAGS_FILE_NAME;

        if (messageId == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        resp.addHeader("Content-Disposition", "attachment; filename=" + fileName);

        final File bodyFile = Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId, fileName).toFile(), tagsFile = Paths
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
                object.put("tags", IOUtils.toString(new FileInputStream(tagsFile)));
            object.put("content_html", IOUtils.toString(new FileInputStream(bodyFile)));
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (JSONException e) {
            e.printStackTrace(System.err);
            //Should never happen
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (resp.getContentType() == null || !resp.getContentType().toLowerCase().contentEquals("application/json")) {
            resp.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
            return;
        }

        final JSONObject object = (JSONObject) JSON.parse(req.getReader()); //Data from the body as characters (fine
        // for JSON)
        final String content_html;
        final List<String> tags = new LinkedList<>();
        try {
            content_html = object.getString("content_html");
            JSONArray tagsAsJSONArray = object.getJSONArray("tags");
            for (Integer i = 0; i < tagsAsJSONArray.length(); i++)
                tags.add(tagsAsJSONArray.getString(i));
        } catch (JSONException e) {
            e.printStackTrace(System.err);
            //Should never happen
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (MessageManagerSingleton.getInstance().areMoreMessagesAllowed()) {
            if (MessageManagerSingleton.getInstance().processMessage(content_html, tags)) {
                resp.setStatus(HttpServletResponse.SC_OK);
                //TODO Notify the GCM server so that it asks for the proper tags to be refreshed
            } else
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
