// Tests whether inferring an @Sibling1 annotation when another
// @Sibling1 annotation from a different namespace is present causes
// problems.

class ConflictingAnnotationsTest {

    int getWPINamespaceSibling1() {
        return MethodReturnTest.getSibling1();
    }

    // This version of Sibling1 is not typechecked - it doesn't
    // belong to the checker and instead is defined in the Sibling1.java
    // file in this directory.
    @Sibling1 int getLocalSibling1() {
        return 1;
    }
}
