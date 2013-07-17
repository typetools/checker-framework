import checkers.nullness.quals.*;


/**
 * Test case for issue 53: http://code.google.com/p/checker-framework/issues/detail?id=53
 * @skip-test
 */
public class AssertIfTrueTest2 {

    private @Nullable Long id;
    public @Pure @Nullable Long getId(){
        return id;
    }
    @AssertNonNullIfTrue("getId()")
    public boolean hasId2(){
        return getId() != null;
    }

    @AssertNonNullIfTrue("id")
    public boolean hasId11(){
        return id != null;
    }
    @AssertNonNullIfTrue("id")
    public boolean hasId12(){
        return this.id != null;
    }
    @AssertNonNullIfTrue("this.id")
    public boolean hasId13(){
        return id != null;
    }
    @AssertNonNullIfTrue("this.id")
    public boolean hasId14(){
        return this.id != null;
    }

    void client() {
        if (hasId11()) { id.toString(); }
        if (hasId12()) { id.toString(); }
        if (hasId13()) { id.toString(); }
        if (hasId14()) { id.toString(); }
        //:: error: (dereference.of.nullable)
        id.toString();
    }

}
