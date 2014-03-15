package java.lang;

@SuppressWarnings("try")
public interface AutoCloseable {
    void close() throws Exception;
}
