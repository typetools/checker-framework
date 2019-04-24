import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class OffsetAnnotations {
    public static void OffsetUnsoundness(String[] args) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        char[] buffer = new char[10];
        bufferedReader.read(buffer, 5, 7);
    }
}
