package org.jorge.lolin1.utils;

import org.jorge.lolin1.data.DataUpdater;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This file is part of lolin1-data-provider.
 * <p/>
 * lolin1-data-provider is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * lolin1-data-provider is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with lolin1-data-provider.  If not, see <http://www.gnu.org/licenses/>.
 */
public abstract class LoLin1DataProviderUtils {

    private static final String API_KEY = "YOUR_API_KEY_HERE",
            API_PARAM_NAME = "api_key";

    public static boolean delete(String pathToFile) {
        File file, fileAux;

        if ((file = new File(pathToFile)).exists()) {
            try {
                for (String target : file.list()) {
                    if ((fileAux = new File(target)).isFile()) {
                        if (!fileAux.delete()) {
                            return false;
                        }
                    } else {
                        LoLin1DataProviderUtils.delete(Paths.get(pathToFile, target).toString());
                    }
                }
            } catch (NullPointerException ex) {
            }
            return file.delete();
        }
        return false;
    }

    public static final synchronized String performRiotGet(String url) {
        StringBuilder ret = new StringBuilder();
        URL obj = null;
        try {
            obj = new URL(url + ((url.contains("=")) ? "&" : "?")
                    + LoLin1DataProviderUtils.API_PARAM_NAME + "=" + LoLin1DataProviderUtils.API_KEY);
        } catch (MalformedURLException e) {
            e.printStackTrace(System.err);
        }

        HttpsURLConnection con = null;
        try {
            con = (HttpsURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }

        try {
            if (con.getResponseCode() == 200) {

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                ret.append(response.toString());
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            try {
                Thread.sleep(DataUpdater.getRetryDelayMillis());
            } catch (InterruptedException e1) {
                e1.printStackTrace(System.err);
            }
            return LoLin1DataProviderUtils.performRiotGet(url);
        }

        return ret.toString();
    }

    public static String readFile(Path path) {
        String pathToFile = path.toString(), line = "";
        try (BufferedReader br = new BufferedReader(new FileReader(pathToFile))) {
            while (true) {
                try {
                    line = line.concat(br.readLine());
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                } catch (NullPointerException ex) {
                    break;
                }
                line = line.concat("\n");
            }
        } catch (final IOException ex) {
            if (ex instanceof FileNotFoundException) {
                return null;
            } else {
                ex.printStackTrace(System.err);
            }
        }

        try {
            return line.substring(0, line.length() - 1);
        } catch (final IndexOutOfBoundsException ex) {
            // If the file is empty.
            return "";
        }
    }

    public static String toSystemJSON(String contentType, String content) {
        StringBuilder ret = new StringBuilder("{\"status\":\"ok\"");
        ret.append(",\"" + contentType + "\":\"" + content + "\"");
        ret.append("}");
        return ret.toString();
    }

    public static void writeFile(Path path, String contents) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path.toString(),
                Boolean.FALSE))) {
            pw.write(contents);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }
}
