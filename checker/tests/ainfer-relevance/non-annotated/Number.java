// This class exists so that the unnamed package declares a type whose name is also the name of a
// type in `java.lang` and of a nested type in `LexicallyNestedType`.  Because this compilation
// unit is in the unnamed package, the simple name "Number" is also this type's fully-qualified
// name.  Inference must not resolve every occurrence of the name "Number" to this type; a type
// that is in scope takes precedence over a type whose fully-qualified name is "Number".
//
// This class is irrelevant, because it is not a subtype of any type in the checker's
// `@RelevantJavaTypes`.
public class Number {}
