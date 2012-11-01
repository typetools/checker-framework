import checkers.nullness.quals.*;

/**
 * Documented in Issue 62
 */
public class AssertIfNonNullTest {

    Long id;

    public AssertIfNonNullTest(Long id) {
        this.id = id;
    }

    @AssertNonNullIfNonNull("id")
    public @checkers.quals.Pure @Nullable Long getId(){
        return id;
    }

}
