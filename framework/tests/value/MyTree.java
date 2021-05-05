// Test case for Issue #2686
// https://github.com/typetools/checker-framework/issues/2686

package issue2686;

import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.qual.UnknownVal;

public class MyTree<Value> {

  public static <V> MyTree<V> newTree(V value) {
    throw new Error("body doesn't matter");
  }

  public MyTree<Value> put(Value newValue) {
    throw new Error("body doesn't matter");
  }

  void uses() {
    newTree("hello").put("bye");

    MyTree<@UnknownVal String> myTree1 = newTree("hello").put("bye");
    // :: error: (assignment)
    MyTree<@StringVal("hello") String> myTree1b = newTree("hello").put("bye");

    // Note: This is a false positive: the type of newTree("hello").put("hello") should be
    // inferred as MyTree<@StringVal("hello") String> and the assignment should therefore pass.
    // :: error: (assignment)
    MyTree<@StringVal("hello") String> myTree2 = newTree("hello").put("hello");
    MyTree<@StringVal("hello") String> myTree2b =
        MyTree.<@StringVal("hello") String>newTree("hello").put("hello");
    // :: error: (assignment)
    MyTree<@StringVal("error") String> myTree2c = newTree("hello").put("hello");

    MyTree<@UnknownVal String> myTree3 = newTree("hello");
    myTree3.put("bye");

    MyTree<@StringVal("hello") String> myTree4 = newTree("hello");
    // :: error: (argument)
    myTree4.put("bye");
  }
}
