// Test case for issue 3668:
// https://github.com/typetools/checker-framework/issues/3668

import org.checkerframework.checker.signedness.qual.*;

public class ObjectCasts {

    Integer castObjectToInteger1(Object o) {
        // :: error: (return.type.incompatible)
        return (Integer) o;
    }

    Integer castObjectToInteger2(@Unsigned Object o) {
        // :: error: (return.type.incompatible)
        return (Integer) o;
    }

    Integer castObjectToInteger3(@Signed Object o) {
        return (Integer) o;
    }

    @Signed Integer castObjectToInteger4(Object o) {
        // :: error: (return.type.incompatible)
        return (Integer) o;
    }

    @Signed Integer castObjectToInteger5(@Unsigned Object o) {
        // :: error: (return.type.incompatible)
        return (Integer) o;
    }

    @Signed Integer castObjectToInteger6(@Signed Object o) {
        return (Integer) o;
    }

    @Unsigned Integer castObjectToInteger7(Object o) {
        // :: error: (return.type.incompatible)
        return (Integer) o;
    }

    @Unsigned Integer castObjectToInteger8(@Unsigned Object o) {
        return (Integer) o;
    }

    @Unsigned Integer castObjectToInteger9(@Signed Object o) {
        // :: error: (return.type.incompatible)
        return (Integer) o;
    }

    Object castIntegerToObject1(Integer o) {
        return (Object) o;
    }

    Object castIntegerToObject2(@Unsigned Integer o) {
        return (Object) o;
    }

    Object castIntegerToObject3(@Signed Integer o) {
        return (Object) o;
    }

    @Signed Object castIntegerToObject4(Integer o) {
        return (Object) o;
    }

    @Signed Object castIntegerToObject5(@Unsigned Integer o) {
        // :: error: (return.type.incompatible)
        return (Object) o;
    }

    @Signed Object castIntegerToObject6(@Signed Integer o) {
        return (Object) o;
    }

    @Unsigned Object castIntegerToObject7(Integer o) {
        // :: error: (return.type.incompatible)
        return (Object) o;
    }

    @Unsigned Object castIntegerToObject8(@Unsigned Integer o) {
        return (Object) o;
    }

    @Unsigned Object castIntegerToObject9(@Signed Integer o) {
        // :: error: (return.type.incompatible)
        return (Object) o;
    }

    void castObjectToBoxedVariants() {
        byte b1 = 1;
        short s1 = 1;
        int i1 = 1;
        long l1 = 1;
        Object[] obj = new Object[] {b1, s1, i1, l1};
        // :: error: (argument.type.incompatible)
        byteParameter((Byte) obj[0]);
        // :: error: (argument.type.incompatible)
        shortParameter((Short) obj[1]);
        // :: error: (argument.type.incompatible)
        integralParameter((Integer) obj[2]);
        // :: error: (argument.type.incompatible)
        longParameter((Long) obj[3]);
    }

    void byteParameter(byte b) {}

    void shortParameter(short s) {}

    void integralParameter(int i) {}

    void longParameter(long l) {}
}
