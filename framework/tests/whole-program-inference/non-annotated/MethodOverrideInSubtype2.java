import org.checkerframework.framework.testchecker.wholeprograminference.qual.Sibling2;

abstract class MethodOverrideInSubtype2 extends MethodDefinedInSupertype {

    private @Sibling2 int getSibling2() {
        return 0;
    }

    @java.lang.Override
    public int shouldReturnParent() {
        return getSibling2();
    }
}
