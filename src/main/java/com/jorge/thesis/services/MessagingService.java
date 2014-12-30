package com.jorge.thesis.services;

import com.jorge.thesis.util.ConfigVars;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

public final class MessagingService extends HttpServlet {

    private static final long serialVersionUID = -9034267862516901563L;
    private static final String TAG_SEPARATOR = "+";

    @Path("/{messageid}/{filename}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    Response uploadFile(@PathParam("messageid") String messageId, @PathParam("filename") String fileName)
            throws ServletException, IOException {
        File file = Paths.get(ConfigVars.MESSAGE_CONTAINER, messageId, fileName).toFile();
        return null;
//TODO Upload file
//        if (file == null || !file.exists())
//            return Response.noContent().header("Content-Disposition", "attachment; filename=" + fileName)
//                    .header("Message-Identifier", "string; identifier=" + messageId).build();
//
//        return Response.ok(file).header("Content-Disposition", "attachment; filename=" + file.getName()).build();
    }

    @Override
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
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
    @Consumes("application/json")
    @Produces("application/json")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //TODO Upload message
    }
}
