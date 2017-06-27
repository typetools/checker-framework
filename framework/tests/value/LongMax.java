import org.checkerframework.common.value.qual.*;
// Test for handling value annotations involving Long.MAX_VALUE
public class LongMax {

    public void longMaxRange() {
        @IntRange(from = 9223372036854775807l, to = 9223372036854775807l) long i = 9223372036854775807l;
        @IntRange(from = 9223372036854775807l, to = 9223372036854775807l) long j = i;
    }
}
