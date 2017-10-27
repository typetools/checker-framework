import org.checkerframework.checker.nullness.qual.*;

/*
 * Tests parsing annotations on parameter represented by an array or vararg to the constructor.
 */
//warning: StubParser: Type not found: com.sun.javadoc.ClassDoc
//warning: StubParser: Type not found: com.sun.javadoc.Doc
//warning: StubParser: Type not found: com.sun.javadoc.FieldDoc
//warning: StubParser: Type not found: com.sun.javadoc.MemberDoc
//warning: StubParser: Type not found: com.sun.javadoc.ProgramElementDoc
//warning: StubParser: Type not found: com.sun.javadoc.RootDoc
class ProcessBuilding2 {

    public void strArraysNonNull(@NonNull String[] parameter) {
        new ProcessBuilder(parameter);
    }

    public void strArraysNullable(@Nullable String[] parameter) {
        //:: error: (argument.type.incompatible)
        new ProcessBuilder(parameter);
    }

    public void strVarargNonNull(@NonNull String... parameter) {
        new ProcessBuilder(parameter);
    }

    public void strVarargNullable(@Nullable String... parameter) {
        //:: error: (argument.type.incompatible)
        new ProcessBuilder(parameter);
    }
}
