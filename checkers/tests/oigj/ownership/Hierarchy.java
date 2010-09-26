import checkers.oigj.quals.*;

/**
 * Smoke tests for testing the hierarchy of ownership part of OIGJ
 */
public class Hierarchy {

    @Dominator Object dominator;
    @Modifier  Object modifier;

    void dominator() {
        dominator = dominator;
        //:: (assignment.type.incompatible)
        modifier = dominator;
    }

    void modifier() {
        dominator = modifier;
        modifier = modifier;
    }
}
