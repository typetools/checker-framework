import org.checkerframework.checker.nullness.qual.*;

// Enable this test once AssertNonNullIfNonNull is implemented
// @skip-test

/**
 * Documented in Issue 62
 */
public class AssertIfNonNullTest {

    Long id;

    public AssertIfNonNullTest(Long id) {
        this.id = id;
    }

    @AssertNonNullIfNonNull("id")
    @Pure
    public @Nullable Long getId() {
        return id;
    }

}
