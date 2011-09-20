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

    protected int next(@ReadOnly Random this, int bits) {
      throw new RuntimeException("skeleton method");
    }

    public void nextBytes(@ReadOnly Random this, byte @ReadOnly [] bytes) {
      throw new RuntimeException("skeleton method");
    }

    public int nextInt(@ReadOnly Random this) {
      throw new RuntimeException("skeleton method");
    }

    public int nextInt(@ReadOnly Random this, int n) {
      throw new RuntimeException("skeleton method");
    }

    public long nextLong(@ReadOnly Random this) {
      throw new RuntimeException("skeleton method");
    }

    public boolean nextBoolean(@ReadOnly Random this) {
      throw new RuntimeException("skeleton method");
    }

    public float nextFloat(@ReadOnly Random this) {
      throw new RuntimeException("skeleton method");
    }

    public double nextDouble(@ReadOnly Random this) {
      throw new RuntimeException("skeleton method");
    }

    synchronized public double nextGaussian(@ReadOnly Random this) {
      throw new RuntimeException("skeleton method");
    }
}
