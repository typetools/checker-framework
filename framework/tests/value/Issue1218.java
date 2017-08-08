// Test case for Issue 1218:
// https://github.com/typetools/checker-framework/issues/1218
import java.io.Serializable;
import org.checkerframework.common.value.qual.*;

class Issue1218 {

    enum MyEnum {
        A,
        B,
        C;
    }

    class ForString {
        ForString(String @MinLen(2) ... strs) {}
    }

    class ForInt {
        ForInt(@IntVal({1, 2, 3}) int @MinLen(2) ... strs) {}
    }

    class ForEnum<E extends Enum<E>> {
        @SafeVarargs
        ForEnum(E @MinLen(2) ... enums) {}
    }

    class ForAny<T> {
        @SafeVarargs
        ForAny(T @MinLen(3) ... anys) {}
    }

    void strs(String @MinLen(2) ... strs) {}

    void ints(@IntVal({1, 2, 3}) int @MinLen(2) ... ints) {}

    @SafeVarargs
    final <E extends Enum<E>> void enums(E @MinLen(2) ... enums) {}

    @SafeVarargs
    final <T> void anys(T @MinLen(3) ... anys) {}

    void testMethodCall() {
        //:: error: (argument.type.incompatible)
        strs();
        //:: error: (argument.type.incompatible)
        strs("");
        strs("", "");
        // type of arg should be @UnknownVal String @BottomVal []
        strs((String[]) null);

        String[] args0 = {""};
        String[] args1 = {""};
        String[] args2 = {"", ""};

        //:: error: (argument.type.incompatible)
        strs(args0);
        //:: error: (argument.type.incompatible)
        strs(args1);
        strs(args2);

        ints(1, 2);
        //:: error: (argument.type.incompatible)
        ints(0, 0, 0);
        //:: error: (argument.type.incompatible)
        ints(3);
        // type of arg should be @IntVal(1) int @BottomVal []
        ints((@IntVal(1) int[]) null);
    }

    void testMethodCallTypeInferred() {
        //:: error: (argument.type.incompatible)
        enums();
        //:: error: (argument.type.incompatible)
        enums(MyEnum.A);
        enums(MyEnum.A, MyEnum.B);
        enums(MyEnum.A, MyEnum.B, MyEnum.C);
    }

    <T extends Comparable<T> & Serializable> void testMethodCallTypeInferredIntersection() {
        T t = null;

        //:: error: (argument.type.incompatible)
        anys(1, 1.0);
        anys(1, 1.0, "");
        //:: error: (argument.type.incompatible)
        anys(1, t);
        anys(1, t, "");
    }

    void testConstructorCall() {
        //:: error: (argument.type.incompatible)
        new ForString();
        //:: error: (argument.type.incompatible)
        new ForString("");
        new ForString("", "");
        // type of arg should be @UnknownVal String @BottomVal []
        new ForString((String[]) null);

        String[] args0 = {""};
        String[] args1 = {""};
        String[] args2 = {"", ""};

        //:: error: (argument.type.incompatible)
        new ForString(args0);
        //:: error: (argument.type.incompatible)
        new ForString(args1);
        new ForString(args2);

        new ForInt(1, 2);
        //:: error: (argument.type.incompatible)
        new ForInt(0, 0, 0);
        //:: error: (argument.type.incompatible)
        new ForInt(3);
        // type of arg should be @IntVal(1) int @BottomVal []
        ints((@IntVal(1) int[]) null);
    }

    void testConstructorCallTypeInferred() {
        //:: error: (argument.type.incompatible)
        new ForEnum<>(MyEnum.A);
        new ForEnum<>(MyEnum.A, MyEnum.B);
        new ForEnum<>(MyEnum.A, MyEnum.B, MyEnum.C);
    }

    @SuppressWarnings("unchecked")
    <T extends Comparable<T> & Serializable> void testConstructorCallTypeInferredIntersection() {
        T t = null;

        //:: error: (argument.type.incompatible)
        new ForAny<>(1, 1.0);
        new ForAny<>(1, 1.0, "");
        //:: error: (argument.type.incompatible)
        new ForAny<>(1, t);
        new ForAny<>(1, t, "");
    }
}
