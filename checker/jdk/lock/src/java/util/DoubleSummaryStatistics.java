package java.util;

import org.checkerframework.checker.lock.qual.GuardSatisfied;
import java.util.function.DoubleConsumer;

public class DoubleSummaryStatistics implements DoubleConsumer {
    public DoubleSummaryStatistics() { throw new RuntimeException("skeleton method"); }
    @Override
    public void accept(double arg0) { throw new RuntimeException("skeleton method"); }
    public void combine(DoubleSummaryStatistics arg0) { throw new RuntimeException("skeleton method"); }
    public long getCount(@GuardSatisfied DoubleSummaryStatistics this) { throw new RuntimeException("skeleton method"); }
    public double getSum(@GuardSatisfied DoubleSummaryStatistics this) { throw new RuntimeException("skeleton method"); }
    public double getMin(@GuardSatisfied DoubleSummaryStatistics this) { throw new RuntimeException("skeleton method"); }
    public double getMax(@GuardSatisfied DoubleSummaryStatistics this) { throw new RuntimeException("skeleton method"); }
    public double getAverage(@GuardSatisfied DoubleSummaryStatistics this) { throw new RuntimeException("skeleton method"); }
    @Override
    public String toString() { throw new RuntimeException("skeleton method"); }
}
