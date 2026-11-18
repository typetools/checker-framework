// Tests that dataflow analysis terminates for nested loops whose stores contain a value of
// type-variable type with no accumulated values.  The analysis reaches a fixed point only if the
// AccumulationValue at the loop head does not alternate between a null and an empty set of
// accumulated values.

import java.util.List;

public class AccumulationValueEmptySetLoop {

  interface Item {
    boolean flag();

    String text();
  }

  <T extends Item> void nestedLoops(List<T> items, List<Object> objects) {
    for (T item : items) {
      for (Object o : objects) {
        if (o != null) {
          if (item.flag()) {
            System.err.printf("%s %s %s", o, item.text(), item);
          }
        }
      }
    }
  }
}
