import java.io.File;
import java.io.IOException;

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

            multipart.addHeaderField("User-Agent", "COMM_EXP_SYS_ANDROID");

            multipart.addFilePart("fileUpload", uploadFile1);

            multipart.finish();
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
}
