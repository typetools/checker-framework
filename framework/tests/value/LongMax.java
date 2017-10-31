import org.checkerframework.common.value.qual.IntRange;

// Test for PR 1370: https://github.com/typetools/checker-framework/pull/1370
// This test contains range annotations with both bounds equal to Long.MAX_VALUE
// The Value checker tried to extract a list of values from that range by looping
// from the lower bound to the upper bound. The loop was implemented in a way that
// if the upper bound is Long.MAX_VALUE, the loop condition was always true, the
// loop variable overflowed and OutOfMemoryError was caused by infinitely growing
// the list of values.
public class LongMax {

    public void longMaxRange() {
        @IntRange(from = 9223372036854775807l, to = 9223372036854775807l) long i = 9223372036854775807l;
        @IntRange(from = 9223372036854775807l, to = 9223372036854775807l) long j = i;
    }
}
