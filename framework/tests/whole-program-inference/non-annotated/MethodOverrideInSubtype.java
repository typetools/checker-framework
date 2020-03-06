import testlib.wholeprograminference.qual.Sibling1;

class MethodOverrideInSubtype extends MethodDefinedInSupertype {
    @java.lang.Override
    public int shouldReturnSibling1() {
        return getSibling1();
    }

    private @Sibling1 int getSibling1() {
        return 0;
    }

    @java.lang.Override
    public int shouldReturnParent() {
        return getSibling1();
    }
}
