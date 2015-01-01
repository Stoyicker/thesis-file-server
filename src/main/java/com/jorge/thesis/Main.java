package com.jorge.thesis;

import com.jorge.thesis.io.files.PurgerSingleton;
import com.jorge.thesis.services.FilesService;
import com.jorge.thesis.services.MessagesService;
import com.jorge.thesis.services.TagService;
import com.jorge.thesis.util.ConfigVars;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

public class Main {

    private static final Integer DEFAULT_PORT = 61000, MINIMUM_PORT = 50000, MAXIMUM_PORT = 65000;

    public static void main(String[] args) throws Exception {
        ConfigVars.init();

        final Path messageFolder = Paths.get(ConfigVars.MESSAGE_CONTAINER);

        if (!Files.exists(messageFolder)) {
            if (messageFolder.toFile().mkdirs()) {
                System.out.println("Message folder not found. Created.");
            } else
                throw new IllegalStateException("Message folder not found and could not create it. Startup aborted.");
        }

        final PurgerSingleton purger = PurgerSingleton.getInstance();
        if (purger.parse())
            purger.runPurge();

        Integer webPort;
        try {
            webPort = Integer.valueOf(ConfigVars.PORT);
            if (webPort < MINIMUM_PORT) {
                throw new NumberFormatException(MessageFormat.format("Invalid port, use a number between {0} and " +
                        "{1}", MINIMUM_PORT, MAXIMUM_PORT));
            }
        } catch (NumberFormatException ex) {
            webPort = DEFAULT_PORT;
        }

        Server server = new Server(webPort);
        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new MessagesService()),
                "/messages");
        context.addServlet(new ServletHolder(new FilesService()),
                "/files");
        context.addServlet(new ServletHolder(new TagService()),
                "/tags");

        System.out.print("Requesting server start...");
        server.start();
        System.out.println("done.");

        server.join();
    }
}
