import com.squareup.okhttp.*;
import com.squareup.okhttp.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

public class FileUploadTester {

    private static RequestBody okHttpTestCreate(final MediaType mediaType, final InputStream inputStream) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return mediaType;
            }

            @Override
            public long contentLength() {
                try {
                    return inputStream.available();
                } catch (IOException e) {
                    return 0;
                }
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                Source source = null;
                try {
                    source = Okio.source(inputStream);
                    sink.writeAll(source);
                } finally {
                    Util.closeQuietly(source);
                }
            }
        };
    }

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

    private void okHttpTest() throws IOException {
        OkHttpClient client = new OkHttpClient();

        MediaType MEDIA_TYPE_MULTIPART
                = MediaType.parse("application/octet-stream; charset=utf-8");

        System.out.println(Paths.get(".").toAbsolutePath().toString());
        InputStream inputStream = new FileInputStream("testFile.exe");

        RequestBody requestBody = okHttpTestCreate(MEDIA_TYPE_MULTIPART, inputStream);
        Request request = new Request.Builder()
                .url("http://localhost:61000/files?messageid=1&filename=uploadtest.exe")
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();

        System.out.println("Response status: " + response.code());
        System.out.println("Response body: " + response.body().string());
    }
}
