import typedecldefault.quals.*;

// @TypeDeclDefaultBottom is the default qualifier in hierarchy.
// @TypeDeclDefaultTop is the default for type declarations.
public class TestDefaultForTypeDecl {
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

    <@TypeDeclDefaultBottom T extends @TypeDeclDefaultBottom Object> void genericMethod() {}
}
