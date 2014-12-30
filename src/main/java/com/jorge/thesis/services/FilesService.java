package com.jorge.thesis.services;

import com.jorge.thesis.util.ConfigVars;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FilesService extends HttpServlet {

    private static final long serialVersionUID = -9034267862516901563L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        final String messageId = req.getParameter("messageid"), fileName = req.getParameter("filename");

        if (messageId == null || fileName == null) {
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

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getOutputStream().write(IOUtils.toByteArray(new FileInputStream(file)));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String messageId = req.getParameter("messageid"), fileName = req.getParameter("filename");

        final Path messagePath = Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId);

        if (messageId == null || fileName == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        resp.addHeader("Content-Disposition", "attachment; filename=" + fileName);

        if (Files.exists(messagePath)) {
            if (!Files.exists(Paths.get(messagePath.toAbsolutePath().toString(), fileName))) {
                //TODO Upload file
                resp.setStatus(HttpServletResponse.SC_OK);
            } else
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
        } else
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}
