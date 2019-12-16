import testlib.wholeprograminference.qual.Sibling2;
import testlib.wholeprograminference.qual.Top;
import testlib.wholeprograminference.qual.WholeProgramInferenceBottom;

class UsesAnonymous {
    void method() {
        Anonymous a =
                new Anonymous() {
                    int innerField;

                    public void method2() {
                        Anonymous.field1 = getSibling2();
                        Anonymous.field2 = getSibling2();
                        innerField = getSibling2();
                    }

                    void innerFieldTest() {
                        // :: error: (argument.type.incompatible)
                        expectsSibling2(innerField);
                    }

                    @WholeProgramInferenceBottom int getBottom() {
                        return (@WholeProgramInferenceBottom int) 0;
                    }

                    @Top int getTop() {
                        return (@Top int) 0;
                    }

                    @Sibling2 int getSibling2() {
                        return (@Sibling2 int) 0;
                    }

                    void expectsSibling2(@Sibling2 int t) {}
                };
    }
}
