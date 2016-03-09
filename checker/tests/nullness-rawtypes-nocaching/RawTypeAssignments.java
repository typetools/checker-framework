import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class RawTypeAssignments {
    Map rawMap = new HashMap();
    Map<String, List<String>> notRawMapDiamondRec = new HashMap<>();
    //:: error: (assignment.type.incompatible)
    Map<String, List<String>> notRawMapRawHashMapRec = new HashMap();

    Map<String, CharSequence> notRawMapDiamond = new HashMap<>();
    //:: error: (assignment.type.incompatible)
    Map<String, CharSequence> notRawMapRawHashMap = new HashMap();

    Map<Object, Object> notRawMapDiamondObjectObject = new HashMap<>();
    //:: error: (assignment.type.incompatible)
    Map<Object, Object> notRawMapDiamondObjectObjectRaw = new HashMap();

    RecursiveGeneric rawRecursiveGeneric = new RecursiveGeneric();
    RecursiveGeneric<MyClass> notRawRecursiveGenericDiamond = new RecursiveGeneric<>();
    RecursiveGeneric<MyClass> notRawRecursiveGenericRaw = new RecursiveGeneric();

    class Generic<G extends @Nullable Object> {
    }

    class RecursiveGeneric<R extends Generic<R>> {
    }

    class MyClass extends Generic<MyClass> {
    }
}