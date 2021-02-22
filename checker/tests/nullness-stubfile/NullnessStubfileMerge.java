// warning: stubfile1.astub:(line 16,col 6): Package-private method notReal(String) not found in
// type java.lang.String
// warning: stubfile1.astub:(line 19,col 1): Type not found: java.lang.NotARealClass
// warning: stubfile1.astub:(line 21,col 1): Type not found: not.real.NotARealClassInNotRealPackage

import org.checkerframework.checker.nullness.qual.*;

/*
 * This test reads two stub files:
 * tests/nullness-stubfile/stubfile1.astub
 * tests/nullness-stubfile/stubfile2.astub
 *
 * The annotations on the methods are merged such that reading the two
 * stub files is equavlent to the following stubfile:
 *
public final class  String {
     public @Nullable String intern();
     public @NonNull String substring(@Nullable int beginIndex) @Nullable;
     String(@Nullable String arg0);
     void getChars(@Nullable int arg0, @NonNull int arg1, @NonNull char @NonNull [] arg2, @NonNull int arg3) @NonNull;
}
*/
public class NullnessStubfileMerge {
    @Nullable String nullString = null;
    @NonNull String nonNull = "Hello!";

    void method() {
        // below fails because of stub file overruling annotated JDK
        // :: error: (type.argument.type.incompatible)
        java.util.List<@NonNull String> l;

        // :: error: (assignment.type.incompatible)
        @NonNull String error1 = nonNull.intern();

        nonNull.substring('!');

        @NonNull String y = nonNull.substring('!');

        char[] nonNullChars = {'1', '1'};
        char[] nullChars = null;
        nonNull.getChars(1, 1, nonNullChars, 1);

        // :: error: (argument.type.incompatible)
        nonNull.getChars(1, 1, nullChars, 1);
    }
}
