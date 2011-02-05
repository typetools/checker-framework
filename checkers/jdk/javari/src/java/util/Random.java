package java.util;
import checkers.javari.quals.*;

public
class Random implements java.io.Serializable {
    static final long serialVersionUID = 3905348978240129619L;
    public Random() {
      throw new RuntimeException("skeleton method");
    }
    public Random(long seed) {
      throw new RuntimeException("skeleton method");
    }

    synchronized public void setSeed(long seed) {
      throw new RuntimeException("skeleton method");
    }

  protected int next(int bits) @ReadOnly {
      throw new RuntimeException("skeleton method");
    }

    public void nextBytes(byte @ReadOnly [] bytes) /*@ReadOnly*/ {
      throw new RuntimeException("skeleton method");
    }

    public int nextInt() @ReadOnly {
      throw new RuntimeException("skeleton method");
    }

    public int nextInt(int n) @ReadOnly {
      throw new RuntimeException("skeleton method");
    }

    public long nextLong() @ReadOnly {
      throw new RuntimeException("skeleton method");
    }

    public boolean nextBoolean() @ReadOnly {
      throw new RuntimeException("skeleton method");
    }

    public float nextFloat() @ReadOnly {
      throw new RuntimeException("skeleton method");
    }

    public double nextDouble() @ReadOnly {
      throw new RuntimeException("skeleton method");
    }

    synchronized public double nextGaussian() @ReadOnly {
      throw new RuntimeException("skeleton method");
    }
}
