import java.util.List;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.PolyModifiable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;

interface MyGodMap<K, V> {
  // The actual definition.
  V getOrDefault(MyGodMap<K, V> this, Object key, V defaultValue);

  // With @PolyModifiable, which seems wrong.
  @PolyModifiable V getOrDefaultPoly(MyGodMap<K, V> this, Object key, @PolyModifiable V defaultValue);

  // With @PolyModifiable on all three, this seems correct.
  @PolyModifiable V getOrDefaultPoly2(
      MyGodMap<K, @PolyModifiable V> this, Object key, @PolyModifiable V defaultValue);

  // Using a second type variable.
  <V2 extends V> V2 getOrDefault2(MyGodMap<K, V> this, Object key, V2 defaultValue);
}

public class GetOrDefaultTest {

  List<String> d;
  @Modifiable List<String> dm;
  @Unmodifiable List<String> du;

  void foo1(MyGodMap<String, List<String>> m) {
    List<String> d1 = m.getOrDefault("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d2 = m.getOrDefault("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d3 = m.getOrDefault("hello", d);
    List<String> d4 = m.getOrDefault("hello", dm);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d5 = m.getOrDefault("hello", dm);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d6 = m.getOrDefault("hello", dm);
    List<String> d7 = m.getOrDefault("hello", du);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d8 = m.getOrDefault("hello", du);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d9 = m.getOrDefault("hello", du);
  }

  void foo2(MyGodMap<String, @Modifiable List<String>> m) {
    // :: error: [argument] :: error: [argument] :: error: [argument]
    List<String> d1 = m.getOrDefault("hello", d);
    // :: error: [argument] :: error: [argument] :: error: [argument]
    @Modifiable List<String> d2 = m.getOrDefault("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    // :: error: [argument] :: error: [argument] :: error: [argument]
    @Unmodifiable List<String> d3 = m.getOrDefault("hello", d);
    List<String> d4 = m.getOrDefault("hello", dm);
    @Modifiable List<String> d5 = m.getOrDefault("hello", dm);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d6 = m.getOrDefault("hello", dm);
    // :: error: [argument] :: error: [argument] :: error: [argument]
    List<String> d7 = m.getOrDefault("hello", du);
    // :: error: [argument] :: error: [argument] :: error: [argument]
    @Modifiable List<String> d8 = m.getOrDefault("hello", du);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    // :: error: [argument] :: error: [argument] :: error: [argument]
    @Unmodifiable List<String> d9 = m.getOrDefault("hello", du);
  }

  void foo3(MyGodMap<String, @Unmodifiable List<String>> m) {
    // :: error: [argument] :: error: [argument] :: error: [argument]
    List<String> d1 = m.getOrDefault("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    // :: error: [argument] :: error: [argument] :: error: [argument]
    @Modifiable List<String> d2 = m.getOrDefault("hello", d);
    // :: error: [argument] :: error: [argument] :: error: [argument]
    @Unmodifiable List<String> d3 = m.getOrDefault("hello", d);
    // :: error: [argument] :: error: [argument] :: error: [argument]
    List<String> d4 = m.getOrDefault("hello", dm);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    // :: error: [argument] :: error: [argument] :: error: [argument]
    @Modifiable List<String> d5 = m.getOrDefault("hello", dm);
    // :: error: [argument] :: error: [argument] :: error: [argument]
    @Unmodifiable List<String> d6 = m.getOrDefault("hello", dm);
    List<String> d7 = m.getOrDefault("hello", du);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d8 = m.getOrDefault("hello", du);
    @Unmodifiable List<String> d9 = m.getOrDefault("hello", du);
  }

  void foo4(MyGodMap<String, List<String>> m) {
    List<String> d1 = m.getOrDefaultPoly("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d2 = m.getOrDefaultPoly("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d3 = m.getOrDefaultPoly("hello", d);
    List<String> d4 = m.getOrDefaultPoly("hello", dm);
    @Modifiable List<String> d5 = m.getOrDefaultPoly("hello", dm);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d6 = m.getOrDefaultPoly("hello", dm);
    List<String> d7 = m.getOrDefaultPoly("hello", du);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d8 = m.getOrDefaultPoly("hello", du);
    @Unmodifiable List<String> d9 = m.getOrDefaultPoly("hello", du);
  }

  void foo5(MyGodMap<String, @Modifiable List<String>> m) {
    List<String> d1 = m.getOrDefaultPoly("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d2 = m.getOrDefaultPoly("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d3 = m.getOrDefaultPoly("hello", d);
    List<String> d4 = m.getOrDefaultPoly("hello", dm);
    @Modifiable List<String> d5 = m.getOrDefaultPoly("hello", dm);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d6 = m.getOrDefaultPoly("hello", dm);
    List<String> d7 = m.getOrDefaultPoly("hello", du);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d8 = m.getOrDefaultPoly("hello", du);
    @Unmodifiable List<String> d9 = m.getOrDefaultPoly("hello", du);
  }

  void foo6(MyGodMap<String, @Unmodifiable List<String>> m) {
    List<String> d1 = m.getOrDefaultPoly("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d2 = m.getOrDefaultPoly("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d3 = m.getOrDefaultPoly("hello", d);
    List<String> d4 = m.getOrDefaultPoly("hello", dm);
    @Modifiable List<String> d5 = m.getOrDefaultPoly("hello", dm);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d6 = m.getOrDefaultPoly("hello", dm);
    List<String> d7 = m.getOrDefaultPoly("hello", du);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d8 = m.getOrDefaultPoly("hello", du);
    @Unmodifiable List<String> d9 = m.getOrDefaultPoly("hello", du);
  }

  void foo7(MyGodMap<String, List<String>> m) {
    List<String> d1 = m.getOrDefault2("hello", d);
    // :: error: [assignment] :: error: [type.arguments.not.inferred] :: error: [assignment] ::
    // error: [type.arguments.not.inferred] :: error: [assignment] :: error:
    // [type.arguments.not.inferred]
    @Modifiable List<String> d2 = m.getOrDefault2("hello", d);
    // :: error: [assignment] :: error: [type.arguments.not.inferred] :: error: [assignment] ::
    // error: [type.arguments.not.inferred] :: error: [assignment] :: error:
    // [type.arguments.not.inferred]
    @Unmodifiable List<String> d3 = m.getOrDefault2("hello", d);
    List<String> d4 = m.getOrDefault2("hello", dm);
    @Modifiable List<String> d5 = m.getOrDefault2("hello", dm);
    // :: error: [assignment] :: error: [type.arguments.not.inferred] :: error: [assignment] ::
    // error: [type.arguments.not.inferred] :: error: [assignment] :: error:
    // [type.arguments.not.inferred]
    @Unmodifiable List<String> d6 = m.getOrDefault2("hello", dm);
    List<String> d7 = m.getOrDefault2("hello", du);
    // :: error: [assignment] :: error: [type.arguments.not.inferred] :: error: [assignment] ::
    // error: [type.arguments.not.inferred] :: error: [assignment] :: error:
    // [type.arguments.not.inferred]
    @Modifiable List<String> d8 = m.getOrDefault2("hello", du);
    @Unmodifiable List<String> d9 = m.getOrDefault2("hello", du);
  }

  void foo8(MyGodMap<String, @Modifiable List<String>> m) {
    // :: error: [type.arguments.not.inferred] :: error: [type.arguments.not.inferred] :: error:
    // [type.arguments.not.inferred]
    List<String> d1 = m.getOrDefault2("hello", d);
    // :: error: [assignment] :: error: [type.arguments.not.inferred] :: error: [assignment] ::
    // error: [type.arguments.not.inferred] :: error: [assignment] :: error:
    // [type.arguments.not.inferred]
    @Modifiable List<String> d2 = m.getOrDefault2("hello", d);
    // :: error: [assignment] :: error: [type.arguments.not.inferred] :: error: [assignment] ::
    // error: [type.arguments.not.inferred] :: error: [assignment] :: error:
    // [type.arguments.not.inferred]
    @Unmodifiable List<String> d3 = m.getOrDefault2("hello", d);
    List<String> d4 = m.getOrDefault2("hello", dm);
    @Modifiable List<String> d5 = m.getOrDefault2("hello", dm);
    // :: error: [assignment] :: error: [type.arguments.not.inferred] :: error: [assignment] ::
    // error: [type.arguments.not.inferred] :: error: [assignment] :: error:
    // [type.arguments.not.inferred]
    @Unmodifiable List<String> d6 = m.getOrDefault2("hello", dm);
    // :: error: [type.arguments.not.inferred] :: error: [type.arguments.not.inferred] :: error:
    // [type.arguments.not.inferred]
    List<String> d7 = m.getOrDefault2("hello", du);
    // :: error: [assignment] :: error: [type.arguments.not.inferred] :: error: [assignment] ::
    // error: [type.arguments.not.inferred] :: error: [assignment] :: error:
    // [type.arguments.not.inferred]
    @Modifiable List<String> d8 = m.getOrDefault2("hello", du);
    // :: error: [type.arguments.not.inferred] :: error: [type.arguments.not.inferred] :: error:
    // [type.arguments.not.inferred]
    @Unmodifiable List<String> d9 = m.getOrDefault2("hello", du);
  }

  void foo9(MyGodMap<String, @Unmodifiable List<String>> m) {
    // :: error: [type.arguments.not.inferred] :: error: [type.arguments.not.inferred] :: error:
    // [type.arguments.not.inferred]
    List<String> d1 = m.getOrDefault2("hello", d);
    // :: error: [assignment] :: error: [type.arguments.not.inferred] :: error: [assignment] ::
    // error: [type.arguments.not.inferred] :: error: [assignment] :: error:
    // [type.arguments.not.inferred]
    @Modifiable List<String> d2 = m.getOrDefault2("hello", d);
    // :: error: [assignment] :: error: [type.arguments.not.inferred] :: error: [assignment] ::
    // error: [type.arguments.not.inferred] :: error: [assignment] :: error:
    // [type.arguments.not.inferred]
    @Unmodifiable List<String> d3 = m.getOrDefault2("hello", d);
    // :: error: [type.arguments.not.inferred] :: error: [type.arguments.not.inferred] :: error:
    // [type.arguments.not.inferred]
    List<String> d4 = m.getOrDefault2("hello", dm);
    // :: error: [type.arguments.not.inferred] :: error: [type.arguments.not.inferred] :: error:
    // [type.arguments.not.inferred]
    @Modifiable List<String> d5 = m.getOrDefault2("hello", dm);
    // :: error: [assignment] :: error: [type.arguments.not.inferred] :: error: [assignment] ::
    // error: [type.arguments.not.inferred] :: error: [assignment] :: error:
    // [type.arguments.not.inferred]
    @Unmodifiable List<String> d6 = m.getOrDefault2("hello", dm);
    List<String> d7 = m.getOrDefault2("hello", du);
    // :: error: [assignment] :: error: [type.arguments.not.inferred] :: error: [assignment] ::
    // error: [type.arguments.not.inferred] :: error: [assignment] :: error:
    // [type.arguments.not.inferred]
    @Modifiable List<String> d8 = m.getOrDefault2("hello", du);
    @Unmodifiable List<String> d9 = m.getOrDefault2("hello", du);
  }

  // this is the desired behavior.
  void foo10(MyGodMap<String, @Modifiable List<String>> m) {
    List<String> d1 = m.getOrDefaultPoly2("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d2 = m.getOrDefaultPoly2("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d3 = m.getOrDefaultPoly2("hello", d);
    List<String> d4 = m.getOrDefaultPoly2("hello", dm);
    @Modifiable List<String> d5 = m.getOrDefaultPoly2("hello", dm);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d6 = m.getOrDefaultPoly2("hello", dm);
    List<String> d7 = m.getOrDefaultPoly2("hello", du);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d8 = m.getOrDefaultPoly2("hello", du);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d9 = m.getOrDefaultPoly2("hello", du);
  }

  void foo11(MyGodMap<String, @Unmodifiable List<String>> um) {
    List<String> d1 = um.getOrDefaultPoly2("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d2 = um.getOrDefaultPoly2("hello", d);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d3 = um.getOrDefaultPoly2("hello", d);
    List<String> d4 = um.getOrDefaultPoly2("hello", dm);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d5 = um.getOrDefaultPoly2("hello", dm);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Unmodifiable List<String> d6 = um.getOrDefaultPoly2("hello", dm);
    List<String> d7 = um.getOrDefaultPoly2("hello", du);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d8 = um.getOrDefaultPoly2("hello", du);
    @Unmodifiable List<String> d9 = um.getOrDefaultPoly2("hello", du);
  }
}
