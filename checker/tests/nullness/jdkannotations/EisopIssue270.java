import java.util.Set;

public class EisopIssue270 {
    // In annotated jdk, the package-info of java.util defines KeyForBottom as the
    // default qualifier for lower bound.
    void foo(Set<Object> so, Set<? extends Object> seo) {
        // No errors if package-info is loaded correctly.
        so.retainAll(seo);
    }
}
