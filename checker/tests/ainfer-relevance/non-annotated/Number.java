// This class exists so that the unnamed package declares a type whose name is also the name of a
// type in `java.lang` and of a nested type in `LexicallyNestedType`.  Because this compilation
// unit is in the unnamed package, the simple name "Number" is also this type's fully-qualified
// name.  Inference must not resolve every occurrence of the name "Number" to this type; a nested
// type declaration and a type parameter shadow a type of the same name that is declared in the
// same package.  (`SamePackageShadows.java` tests the converse:  where nothing shadows it, a type
// in the same package takes precedence over an import on demand and over `java.lang`.)
//
// This class is irrelevant, because it is not a subtype of any type in the checker's
// `@RelevantJavaTypes`.
public class Number {}
