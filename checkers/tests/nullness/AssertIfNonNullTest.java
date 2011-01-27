import checkers.nullness.quals.*;

/**
 * Documented in Issue 62
 */
public class AssertIfNonNullTest {

    private Long id;
    public @Pure @Nullable Long getId(){
        return id;
    }
    @AssertNonNullIfNonNull("id")
    public boolean hasId2(){
        return id != null;
    }
    // expect an error here! //:: (...)
    @AssertNonNullIfNonNull("id")
    public boolean hasId3(){
        return true;
    }

    
}
