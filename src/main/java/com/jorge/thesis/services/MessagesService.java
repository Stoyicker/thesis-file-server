package com.jorge.thesis.services;

import com.jorge.thesis.datamodel.MessageManagerSingleton;
import com.jorge.thesis.util.ConfigVars;
import org.apache.commons.io.IOUtils;
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

public final class MessagesService extends HttpServlet {

    private static final long serialVersionUID = -9034267862516901563L;
    private static final String TAG_SEPARATOR = "+";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        final String messageId = req.getParameter("messageid"), fileName = ConfigVars.MESSAGE_BODY_FILE_NAME;

        if (messageId == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        resp.addHeader("Content-Disposition", "attachment; filename=" + fileName);

        File file = Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId, fileName).toFile();

        if (!file.exists()) {
            resp.addHeader("Message-Identifier", "string; identifier=" + messageId);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }


        final JSONObject object = new JSONObject();
        try {
            object.put("status", "ok");
            //TODO Puts its tags in a tags attribute separated by TAG_SEPARATOR - object.put("tags",);
            object.put("content_html", IOUtils.toString(new FileInputStream(file)));
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (JSONException e) {
            e.printStackTrace(System.err);
            //Should never happen
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        resp.setContentType("application/json");
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (MessageManagerSingleton.getInstance().areMoreMessagesAllowed()) {
            //TODO Upload message as JSON, store the html
            resp.setStatus(HttpServletResponse.SC_OK);
        } else
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
