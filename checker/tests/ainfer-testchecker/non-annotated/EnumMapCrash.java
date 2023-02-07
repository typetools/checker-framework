// Based on a crash encountered when running WPI with the RLC on Apache Hadoop.

import java.util.EnumMap;

public class EnumMapCrash {
  private class Holder<T> {
    public T held;

    public Holder(T held) {
      this.held = held;
    }

    @Override
    public String toString() {
      return String.valueOf(held);
    }
  }

  private enum FSEditLogOpCodes {}

  void callHolder(FSEditLogOpCodes f, EnumMap<FSEditLogOpCodes, Holder<Integer>> opCounts) {
    Holder<Integer> holder = opCounts.get(f);
    holder.held++;
  }
}
