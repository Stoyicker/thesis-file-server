package com.jorge.thesis.io.files;

import com.jorge.thesis.util.ConfigVars;
import org.apache.commons.io.FileUtils;
import org.jdom2.Element;
import org.jdom2.input.DOMBuilder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class PurgerSingleton {

    private static final Object LOCK = new Object();
    private static volatile PurgerSingleton mInstance;
    private Boolean mRun;
    private Long mDeleteEpoch;
    private List<String> mForbiddenTags;

    public static PurgerSingleton getInstance() {
        PurgerSingleton ret = mInstance;
        if (ret == null) {
            synchronized (LOCK) {
                ret = mInstance;
                if (ret == null) {
                    ret = new PurgerSingleton();
                    mInstance = ret;
                }
            }
        }
        return ret;
    }

    public synchronized Boolean parse() {
        final File purgeFile = Paths.get(ConfigVars.PURGE_CONF).toFile();
        org.jdom2.Document jdomDoc;
        try {
            jdomDoc = useDOMParser(purgeFile);
            final Element root = jdomDoc.getRootElement();
            final Element run = root.getChild("run"), deleteInterval = root.getChild("deleteIfOlderThan"),
                    keepTags = root.getChild("keepTags");
            mRun = Boolean.parseBoolean(run.getText());
            if (!mRun)
                return Boolean.FALSE;
            //86400000 millis in one day
            mDeleteEpoch = System.currentTimeMillis() - Long.parseLong(deleteInterval.getText()) * 30 * 86400000;
            mForbiddenTags = new LinkedList<>();
            final List<Element> forbiddenTags = keepTags.getChildren("tag");
            final Pattern tagFormatPattern = Pattern.compile("[a-z0-9_]+");
            for (Element tag : forbiddenTags) {
                final String cleanTag = tag.getText().trim().toLowerCase(Locale.ENGLISH);
                if (tagFormatPattern.matcher(cleanTag).matches()) {
                    if (!mForbiddenTags.contains(cleanTag)) {
                        mForbiddenTags.add(cleanTag);
                    } else {
                        System.err.println("Duplicated tag " + cleanTag + " in the purger configuration. Skipping.");
                    }
                } else {
                    System.err.println("Malformed tag " + cleanTag + " set for purge protection. Skipping.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            //Should never happen
            System.err.println("Malformed purge configuration file. Skipping purge.");
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    private synchronized org.jdom2.Document useDOMParser(File fileName)
            throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fileName);
        DOMBuilder domBuilder = new DOMBuilder();
        return domBuilder.build(doc);
    }

    public synchronized void runPurge() {
        final File[] fileList = Paths.get(ConfigVars.MESSAGE_CONTAINER).toFile().listFiles();

        if (fileList == null) {
            System.err.println("Error when referencing the message folder. Skipping purge.");
            return;
        }

        for (File x : fileList) {
            if (!x.isDirectory())
                if (!x.delete())
                    System.err.println("Could not delete file " + x.getName() + ". Skipping.");
                else {
                    Long thisFileEpoch;
                    try {
                        thisFileEpoch = Long.parseLong(FileUtils.readFileToString(Paths.get(ConfigVars
                                .MESSAGE_CONTAINER, ConfigVars.MESSAGE_TIMESTAMP_FILE_NAME).toFile()));
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                        System.err.println("Unexpected error when checking for timestamp in directory " + x.getName()
                                + ". Deleting directory " + x.getName() + " (assuming " +
                                "malformation)");
                        if (!FileUtils.deleteQuietly(x))
                            System.err.println("Could not delete directory " + x.getName() + ". Skipping.");
                        continue;
                    }
                    if (mDeleteEpoch > thisFileEpoch) {
                        final List<String> rawTags;
                        try {
                            rawTags = FileUtils.readLines(Paths.get(ConfigVars.MESSAGE_CONTAINER,
                                    ConfigVars.MESSAGE_TAGS_FILE_NAME).toFile(), ConfigVars.SERVER_CHARSET);
                            deleteDirIfTagsAllow(x, rawTags);
                        } catch (IOException e) {
                            e.printStackTrace(System.err);
                            System.err.println("Unexpected error when checking for tags in directory " + x.getName() +
                                    ". Deleting directory " + x.getName() + " (assuming " +
                                    "malformation)");
                            if (!FileUtils.deleteQuietly(x))
                                System.err.println("Could not delete directory " + x.getName() + ". Skipping.");
                        }
                    }
                }
        }
    }

    private void deleteDirIfTagsAllow(File x, List<String> rawTags) {
        if (Collections.disjoint(mForbiddenTags, rawTags)) {
            if (!FileUtils.deleteQuietly(x))
                System.err.println("Could not delete directory " + x.getName() + ". Skipping.");
        }
    }
}
