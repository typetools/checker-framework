package java.io;
import org.checkerframework.checker.javari.qual.*;

public class FileReader extends InputStreamReader {

    public FileReader(String fileName) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public FileReader(File file) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public FileReader(FileDescriptor fd) {
        throw new RuntimeException("skeleton method");
    }

}
