// This class exists so that the unnamed package declares a type whose name is also the name of a
// type that `SamePackageShadows.java` imports on demand and the name of a package-private member
// type that `PackagePrivateNotInherited.java`'s superclass declares.
//
// This class is relevant, because it is a subtype of `CharSequence`, which the checker's
// `@RelevantJavaTypes` lists.  By contrast, `java.util.List` is irrelevant.
public abstract class List implements CharSequence {}
