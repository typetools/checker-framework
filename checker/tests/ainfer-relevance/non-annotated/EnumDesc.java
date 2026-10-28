// This class exists so that the unnamed package declares a type whose name is also the name of the
// member type `EnumDesc` that every enum inherits from `java.lang.Enum`.  It is used by
// `UnnameableEnumInheritsType.java`.
//
// This class is relevant, because it is a subtype of `CharSequence`, which the checker's
// `@RelevantJavaTypes` lists.  By contrast, `java.lang.Enum.EnumDesc` is irrelevant.
public abstract class EnumDesc implements CharSequence {}
