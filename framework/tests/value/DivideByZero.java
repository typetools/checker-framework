package value;

import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntVal;

public class DivideByZero {
    void divideNoException(@DoubleVal(1.0) float f, @DoubleVal(1.0) double d, @IntVal(1) int i) {
        @DoubleVal(Float.POSITIVE_INFINITY) float a = f / 0;
        @DoubleVal(Double.POSITIVE_INFINITY) double b = d / 0l;
        @DoubleVal(Double.POSITIVE_INFINITY) double c = i / 0.0;
        @DoubleVal(Float.POSITIVE_INFINITY) float e = i / 0.0f;

        //:: error: (assignment.type.incompatible)
        @BottomVal float a2 = f / 0;
        //:: error: (assignment.type.incompatible)
        @BottomVal double b2 = d / 0L;
        //:: error: (assignment.type.incompatible)
        @BottomVal double c2 = i / 0.0;
        //:: error: (assignment.type.incompatible)
        @BottomVal float e2 = i / 0.0f;
    }

    void remainderNoException(@DoubleVal(1.0) float f, @DoubleVal(1.0) double d, @IntVal(1) int i) {
        @DoubleVal(Float.NaN) float a = f % 0;
        @DoubleVal(Double.NaN) double b = d % 0L;
        @DoubleVal(Double.NaN) double c = i % 0.0;
        @DoubleVal(Float.NaN) float e = i % 0.0f;

        //:: error: (assignment.type.incompatible)
        @BottomVal float a2 = f % 0;
        //:: error: (assignment.type.incompatible)
        @BottomVal double b2 = d % 0l;
        //:: error: (assignment.type.incompatible)
        @BottomVal double c2 = i % 0.0;
        //:: error: (assignment.type.incompatible)
        @BottomVal float e2 = i % 0.0f;
    }

    void integerDivision(
            @IntVal(1) long l,
            @IntVal(1) int i,
            @IntVal(0) byte bZero,
            @IntVal(0) short sZero,
            @IntVal(0L) long lZero,
            @IntVal(0) int iZero) {
        @BottomVal long a = l / bZero;
        @BottomVal long b = l / sZero;
        @BottomVal long c = l / iZero;
        @BottomVal long d = l / lZero;

        @BottomVal long e = i / bZero;
        @BottomVal long f = i / sZero;
        @BottomVal long g = i / iZero;
        @BottomVal long h = i / lZero;
    }

    void integerRemainder(
            @IntVal(1) long l,
            @IntVal(1) int i,
            @IntVal(0) byte bZero,
            @IntVal(0) short sZero,
            @IntVal(0L) long lZero,
            @IntVal(0) int iZero) {
        @BottomVal long a = l % bZero;
        @BottomVal long b = l % sZero;
        @BottomVal long c = l % iZero;
        @BottomVal long d = l % lZero;

        @BottomVal long e = i % bZero;
        @BottomVal long f = i % sZero;
        @BottomVal long g = i % iZero;
        @BottomVal long h = i % lZero;
    }
}
