import testlib.wholeprograminference.qual.*;

class OverriddenMethodsTestChildInAnotherCompilationUnit extends OverriddenMethodsTestParent {
    public void callthud(@Sibling1 Object obj1, @Sibling2 Object obj2) {
        thud(obj1, obj2);
    }
}
