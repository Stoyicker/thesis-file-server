import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileUploadTester {

    public static void main(String... args) {
        codeJavaTest();
    }

    private static void codeJavaTest() {
        String charset = "UTF-8";
        File uploadFile1 = new File("testFile.exe");
        String requestURL = "http://localhost:61000/files?messageid=1&filename=uploadtest.exe";

        try {
            MultipartUtility multipart = new MultipartUtility(requestURL, charset);

            multipart.addHeaderField("User-Agent", "CodeJava");
            multipart.addHeaderField("Test-Header", "Header-Value");

            multipart.addFormField("description", "Cool Pictures");
            multipart.addFormField("keywords", "Java,upload,Spring");

            multipart.addFilePart("fileUpload", uploadFile1);

            List<String> response = multipart.finish();

            System.out.println("SERVER REPLIED:");

            for (String line : response) {
                System.out.println(line);
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
}
