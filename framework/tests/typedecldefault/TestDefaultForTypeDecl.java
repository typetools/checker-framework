import typedecldefault.quals.*;

// @TypeDeclDefaultBottom is the default qualifier in hierarchy.
// @TypeDeclDefaultTop is the default for type declarations.
public class TestDefaultForTypeDecl extends @TypeDeclDefaultTop Object {
    class Inner {
        Object o = returnMethod();

        void method() {
            test(TestDefaultForTypeDecl.this);
            testUnannotated(TestDefaultForTypeDecl.this);
        }
    }

    Object returnMethod() {
        return null;
    }

    void test(@TypeDeclDefaultTop TestDefaultForTypeDecl arg) {}

    void testUnannotated(TestDefaultForTypeDecl arg) {}

    void testOtherQual(
            @TypeDeclDefaultBottom TestDefaultForTypeDecl arg, TestDefaultForTypeDecl arg1) {
        arg = arg1;
    }

    void method() {
        Object @TypeDeclDefaultBottom [] object = new Object[] {null};
        this.<Object>genericMethod();
        new TestDefaultForTypeDecl() {};
    }

    Object @TypeDeclDefaultBottom [] object = new Object[] {null};
    @TypeDeclDefaultBottom int i = 1;
    @TypeDeclDefaultBottom String s = "";

    <@TypeDeclDefaultBottom T extends @TypeDeclDefaultBottom Object> void genericMethod() {}
}
