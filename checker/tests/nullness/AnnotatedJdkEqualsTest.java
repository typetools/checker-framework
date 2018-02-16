// @skip-test

// Test case for issue 286: https://github.com/typetools/checker-framework/issues/286

import java.net.URL;

class AnnotatedJdkEqualsTest {
    void foo(URL u) {
        // As of this writing, the annotated JDK does not contain a URL.java file
        // for the java.net.URL class.
        // Nonetheless, the following code should type-check.
        // This could be handled via inheritance of annotations from
        // superclasses either during JDK creation or during type-checking.
        // It would be impractical to manually annotate every method in the
        // entire JDK:  it would be too labor-intensive and there would be
        // certain to be some oversights.
        u.equals(null);
    }
}
