import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RawTypesAssignment {
  Map rawMap = new HashMap();
  Map<String, List<String>> notRawMapDiamondRec = new HashMap<>();
  // :: warning: [unchecked] unchecked conversion
  Map<String, List<String>> notRawMapRawHashMapRec = new HashMap();

  Map<String, CharSequence> notRawMapDiamond = new HashMap<>();
  // :: warning: [unchecked] unchecked conversion
  Map<String, CharSequence> notRawMapRawHashMap = new HashMap();

  Map<Object, Object> notRawMapDiamondObjectObject = new HashMap<>();
  // :: warning: [unchecked] unchecked conversion
  Map<Object, Object> notRawMapDiamondObjectObjectRaw = new HashMap();

  RecursiveGeneric rawRecursiveGeneric = new RecursiveGeneric();
  RecursiveGeneric<MyClass> notRawRecursiveGenericDiamond = new RecursiveGeneric<>();
  // :: warning: [unchecked] unchecked conversion
  RecursiveGeneric<MyClass> notRawRecursiveGenericRaw = new RecursiveGeneric();

  class Generic<G extends @Nullable Object> {}

  class RecursiveGeneric<R extends Generic<R>> {}

  class MyClass extends Generic<MyClass> {}
}
