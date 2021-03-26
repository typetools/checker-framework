public class Parens {
  public synchronized void incrementPushed(long[] pushed, int operationType) {
    ++(pushed[operationType]);
  }
}
