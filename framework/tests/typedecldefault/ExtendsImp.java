import typedecldefault.quals.*;

@SuppressWarnings("super.invocation.invalid") // ignore these
class ExtendsImp {
    static @TypeDeclDefaultBottom class BottomClass {}

    static @TypeDeclDefaultTop class TopClass {}

    static class DefaultClass {}

    // :: error: (declaration.inconsistent.with.extends.clause)
    static class AClass extends BottomClass {}

    static class BClass extends TopClass {}

    static class CClass extends DefaultClass {}

    static @TypeDeclDefaultBottom class DClass extends BottomClass {}

    static @TypeDeclDefaultBottom class EClass extends TopClass {}

    static @TypeDeclDefaultBottom class FClass extends DefaultClass {}

    // :: error: (declaration.inconsistent.with.extends.clause)
    static @TypeDeclDefaultTop class GClass extends BottomClass {}

    static @TypeDeclDefaultTop class HClass extends TopClass {}

    static @TypeDeclDefaultTop class IClass extends DefaultClass {}

    // :: error: (declaration.inconsistent.with.extends.clause)
    static class JClass extends @TypeDeclDefaultBottom DefaultClass {}

    static class KClass extends @TypeDeclDefaultTop DefaultClass {}

    static @TypeDeclDefaultBottom class LClass extends @TypeDeclDefaultTop DefaultClass {}

    static @TypeDeclDefaultBottom class MClass extends @TypeDeclDefaultBottom DefaultClass {}

    static @TypeDeclDefaultTop class NClass extends @TypeDeclDefaultTop DefaultClass {}
    // :: error: (declaration.inconsistent.with.extends.clause)
    static @TypeDeclDefaultTop class OClass extends @TypeDeclDefaultBottom DefaultClass {}

    class Inner {}

    class ExtendInner extends ExtendsImp.@TypeDeclDefaultTop Inner {}
    // :: error: (declaration.inconsistent.with.extends.clause)
    class ExtendInner2 extends ExtendsImp.@TypeDeclDefaultBottom Inner {}

    class ExtendInner3 extends ExtendsImp.Inner {}

    class ExtendInner4 extends @TypeDeclDefaultTop ExtendsImp.Inner {}

    class ExtendInner5 extends @TypeDeclDefaultBottom ExtendsImp.Inner {}
}
