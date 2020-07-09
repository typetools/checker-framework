// Test case for https://tinyurl.com/cfissue/3449

// @skip-test until the issue is fixed

import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class Issue3449 {

    int length;

    public Issue3449(Object... args) {
        length = args.length;
    }
}
