package java.util;

import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public class LongSummaryStatistics implements LongConsumer, IntConsumer {
    public LongSummaryStatistics() { throw new RuntimeException("skeleton method"); }
    @Override
    public void accept(int arg0) { throw new RuntimeException("skeleton method"); }
    @Override
    public void accept(long arg0) { throw new RuntimeException("skeleton method"); }
    public void combine(LongSummaryStatistics arg0) { throw new RuntimeException("skeleton method"); }
    public long getCount() { throw new RuntimeException("skeleton method"); }
    public long getSum() { throw new RuntimeException("skeleton method"); }
    public long getMin() { throw new RuntimeException("skeleton method"); }
    public long getMax() { throw new RuntimeException("skeleton method"); }
    public double getAverage() { throw new RuntimeException("skeleton method"); }
    @Override
    public String toString() { throw new RuntimeException("skeleton method"); }
}
