package com.jorge.thesis.io.files;

import com.jorge.thesis.util.ConfigVars;
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

    public Boolean parse() {
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
            mDeleteEpoch = Long.parseLong(deleteInterval.getText()) * 30 * 86400000; //86400000 millis in one day
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
                    System.err.println("Malformed tag " + cleanTag + ". Skipping.");
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

    private org.jdom2.Document useDOMParser(File fileName)
            throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fileName);
        DOMBuilder domBuilder = new DOMBuilder();
        return domBuilder.build(doc);

    }
}
