// Tests that dataflow analysis terminates for nested loops whose stores contain a value of
// type-variable type with no accumulated values.  The analysis reaches a fixed point only if
// AccumulationValue represents "no accumulated values" in just one way.

import java.util.List;

public class AccumulationValueEmptySetLoop {

  interface Item {
    boolean flag();

    String text();
  }

  void nestedLoops(List<Item> items, List<Object> objects) {
    for (Item item : items) {
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
