import java.io.*;

public class OffsetAnnotations {
    public static void OffsetAnnotationsReader() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        char[] buffer = new char[10];
        // :: error: (argument.type.incompatible)
        bufferedReader.read(buffer, 5, 7);
    }

    public static void OffsetAnnotationsWriter() throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));
        char[] buffer = new char[10];
        // :: error: (argument.type.incompatible)
        bufferedWriter.write(buffer, 5, 7);
    }
}
