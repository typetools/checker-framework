import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.StringVal;

public class StringLenMethods {
    void toString(boolean b, char c, byte y, short s, int i, long l) {

        @StringVal({"true", "false"}) String bs = Boolean.toString(b);
        @ArrayLen(1) String cs = Character.toString(c);
        @ArrayLen({1, 2, 3, 4}) String ys = Byte.toString(y);
        @ArrayLen({1, 2, 3, 4, 5, 6}) String ss = Short.toString(s);
        @ArrayLenRange(from = 1, to = 11) String is = Integer.toString(i);
        @ArrayLenRange(from = 1, to = 20) String ls = Long.toString(l);

        @StringVal({"true", "false"}) String bbs = Boolean.valueOf(b).toString();
        @ArrayLen(1) String bcs = Character.valueOf(c).toString();
        @ArrayLen({1, 2, 3, 4}) String bys = Byte.valueOf(y).toString();
        @ArrayLen({1, 2, 3, 4, 5, 6}) String bss = Short.valueOf(s).toString();
        @ArrayLenRange(from = 1, to = 11) String bis = Integer.valueOf(i).toString();
        @ArrayLenRange(from = 1, to = 20) String bls = Long.valueOf(l).toString();

        // Added in 1.8
        // @ArrayLenRange(from = 1, to = 10) String iu = Integer.toUnsignedString(i);
        // @ArrayLenRange(from = 1, to = 19) String lu = Long.toUnsignedString(l);

        @StringVal({"true", "false"}) String sbs = String.valueOf(b);
        @ArrayLen(1) String scs = String.valueOf(c);
        @ArrayLenRange(from = 1, to = 11) String sis = String.valueOf(i);
        @ArrayLenRange(from = 1, to = 20) String sls = String.valueOf(l);
    }

    void toStringRadix(int i, long l, @IntRange(from = 2, to = 36) int radix) {
        @ArrayLenRange(from = 1) String is = Integer.toString(i, radix);
        @ArrayLenRange(from = 1) String ls = Long.toString(l, radix);

        // Added in 1.8
        // @ArrayLenRange(from = 1) String iu = Integer.toUnsignedString(i, radix);
        // @ArrayLenRange(from = 1) String lu = Long.toUnsignedString(l, radix);

        @ArrayLenRange(from = 1, to = 32) String ib = Integer.toBinaryString(i);
        @ArrayLenRange(from = 1, to = 64) String lb = Long.toBinaryString(l);

        @ArrayLenRange(from = 1, to = 8) String ix = Integer.toHexString(i);
        @ArrayLenRange(from = 1, to = 16) String lx = Long.toHexString(l);

        @ArrayLenRange(from = 1, to = 11) String io = Integer.toOctalString(i);
        @ArrayLenRange(from = 1, to = 22) String lo = Long.toOctalString(l);
    }
}
