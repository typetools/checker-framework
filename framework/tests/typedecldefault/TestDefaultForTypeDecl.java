import typedecldefault.quals.*;

public class TestDefaultForTypeDecl {
    void test(@TypeDeclDefaultTop TestDefaultForTypeDecl arg) {}

    void testUnannotated(TestDefaultForTypeDecl arg) {}

    void testOtherQual(
            @TypeDeclDefaultBottom TestDefaultForTypeDecl arg, TestDefaultForTypeDecl arg1) {
        arg = arg1;
    }
}
