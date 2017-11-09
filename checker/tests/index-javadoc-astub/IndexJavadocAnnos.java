import com.sun.javadoc.RootDoc;
import org.checkerframework.common.value.qual.MinLen;

public class IndexJavadocAnnos {
    void test(IndexJavadocAnnos o, RootDoc root) {
        o.setOptions(root.options());
    }

    void setOptions(Object @MinLen(1) [] objs) {}
}
