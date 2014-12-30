package com.jorge.thesis.services;

import com.jorge.thesis.util.ConfigVars;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
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
import java.util.List;

public final class FilesService extends HttpServlet {

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

        resp.setContentType("application/octet-stream");
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
            final Path filePath;
            if (!Files.exists(filePath = Paths.get(messagePath.toAbsolutePath().toString(), fileName))) {
                if (uploadFile(filePath, req))
                    resp.setStatus(HttpServletResponse.SC_OK);
                else
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
        } else
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    private synchronized Boolean uploadFile(Path filePath, HttpServletRequest req) {
        FileItemFactory factory = new DiskFileItemFactory();

        ServletFileUpload upload = new ServletFileUpload(factory);

        try {
            List items = upload.parseRequest(req);

            // Iterate through the incoming request data
            for (Object item1 : items) {
                // Get the current item in the iteration
                FileItem item = (FileItem) item1;
                if (!item.isFormField()) {
                    File disk = new File(filePath.toAbsolutePath().toString());
                    item.write(disk);
                }
            }

            return Boolean.TRUE;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            //Should never happen
            return Boolean.FALSE;
        }
    }
}
