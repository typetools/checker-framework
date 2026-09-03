// The type factory contains custom inference logic for a class whose name is "IShouldBeSibling1":
// the type factory infers a class declaration annotation for the class.  The inferred annotation
// is a type qualifier, and the .ajava file writes the annotation, with the annotation's
// fully-qualified name, on the class declaration.  Printing the annotation must not crash.
@SuppressWarnings("super.invocation") // Intentional.
public class IShouldBeSibling1 {}
