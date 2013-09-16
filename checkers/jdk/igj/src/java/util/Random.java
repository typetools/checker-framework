package java.util;
import checkers.igj.quals.*;

@Immutable
public class Random implements java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public Random() { throw new RuntimeException("skeleton method"); }
  public Random(long a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void setSeed(long a1) { throw new RuntimeException("skeleton method"); }
  public void nextBytes(byte @ReadOnly [] a1) { throw new RuntimeException("skeleton method"); }
  public int nextInt() { throw new RuntimeException("skeleton method"); }
  public int nextInt(int a1) { throw new RuntimeException("skeleton method"); }
  public long nextLong() { throw new RuntimeException("skeleton method"); }
  public boolean nextBoolean() { throw new RuntimeException("skeleton method"); }
  public float nextFloat() { throw new RuntimeException("skeleton method"); }
  public double nextDouble() { throw new RuntimeException("skeleton method"); }
  public synchronized double nextGaussian() { throw new RuntimeException("skeleton method"); }
}
