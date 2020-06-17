import testlib.wholeprograminference.qual.Sibling1;
import testlib.wholeprograminference.qual.Sibling2;

class OverriddenMethodsTestChildInAnotherCompilationUnit extends OverriddenMethodsTestParent {
    public void callthud(@Sibling1 Object obj1, @Sibling2 Object obj2) {
        thud(obj1, obj2);
    }
}
