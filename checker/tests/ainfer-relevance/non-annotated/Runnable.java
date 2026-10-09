// This class exists so that the unnamed package declares a type whose name is also the name of a
// type in `java.lang`.  It is used by `SamePackageShadows.java`.
//
// This class is relevant, because it is a subtype of `CharSequence`, which the checker's
// `@RelevantJavaTypes` lists.  By contrast, `java.lang.Runnable` is irrelevant.
public abstract class Runnable implements CharSequence {}
