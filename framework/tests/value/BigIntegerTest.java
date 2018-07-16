import java.math.BigInteger;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.PolyValue;

public class BigIntegerTest {
    void construct1(@IntRange(from = -1, to = 1) int signum, byte[] magnitude) {
        BigInteger val = new BigInteger(signum, magnitude);
    }

    void construct2(String val, @IntRange(from = 2, to = 36) int radix) {
        BigInteger value = new BigInteger(val, radix);
    }

    void getDoubleVal(BigInteger val) {
        @PolyValue double dval = val.doubleValue();
    }

    void getIntVal(BigInteger val) {
        @PolyValue int ival = val.intValue();
    }

    void getFloatVal(BigInteger val) {
        @PolyValue float fval = val.floatValue();
    }

    void getLongVal(BigInteger val) {
        @PolyValue double lval = val.longValue();
    }

    void compareTo(BigInteger val, BigInteger to) {
        @IntRange(from = -1, to = 1) int compared = val.compareTo(to);
    }

    void signum(BigInteger val) {
        @IntRange(from = -1, to = 1) int signum = val.signum();
    }
}
