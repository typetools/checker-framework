// Test case for Issue #2686
// https://github.com/typetools/checker-framework/issues/2686

import org.checkerframework.common.value.qual.UnknownVal;

public class MyTree2<Value> {

  public static <V> MyTree2<V> newTree(V value) {
    throw new Error("body doesn't matter");
  }

  public MyTree2<Value> put(String newKey, Value newValue) {
    throw new Error("body doesn't matter");
  }

  public void client1() {
    MyTree2<String> root2 = MyTree2.newTree("constructorarg").put("key1", "value1");
  }

  public void client2() {
    MyTree2<String> root2 =
        MyTree2.newTree((@UnknownVal String) "constructorarg").put("key1", "value1");
  }

  public void client3() {
    MyTree2<String> root2 =
        MyTree2.<String>newTree((@UnknownVal String) "constructorarg").put("key1", "value1");
  }
}
