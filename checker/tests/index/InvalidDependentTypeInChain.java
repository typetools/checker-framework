import org.checkerframework.checker.index.qual.IndexFor;

public class InvalidDependentTypeInChain {

    public InvalidDependentTypeInChain pparent;
    private InvalidDependentTypeInChain parent;

    // Should issue a warning - pparent isn't a valid dependent annotation
    //:: warning: (dependent.not.permitted)
    void test(InvalidDependentTypeInChain chain, @IndexFor("#1.pparent.parent") int x) {}

    //:: warning: (dependent.not.permitted)
    void test1(InvalidDependentTypeInChain chain, @IndexFor("#1.pparent") int x) {}
}
