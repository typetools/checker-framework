package java.util;

import org.checkerframework.checker.lock.qual.GuardSatisfied;
import java.util.function.IntConsumer;

public class IntSummaryStatistics implements IntConsumer {
    public IntSummaryStatistics() { throw new RuntimeException("skeleton method"); }
    @Override
    public void accept(int arg0) { throw new RuntimeException("skeleton method"); }
    public void combine(IntSummaryStatistics arg0) { throw new RuntimeException("skeleton method"); }
    public long getCount(@GuardSatisfied IntSummaryStatistics this) { throw new RuntimeException("skeleton method"); }
    public long getSum(@GuardSatisfied IntSummaryStatistics this) { throw new RuntimeException("skeleton method"); }
    public int getMin(@GuardSatisfied IntSummaryStatistics this) { throw new RuntimeException("skeleton method"); }
    public int getMax(@GuardSatisfied IntSummaryStatistics this) { throw new RuntimeException("skeleton method"); }
    public double getAverage(@GuardSatisfied IntSummaryStatistics this) { throw new RuntimeException("skeleton method"); }
    @Override
    public String toString() { throw new RuntimeException("skeleton method"); }
}
