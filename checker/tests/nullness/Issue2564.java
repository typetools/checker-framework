import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;

public abstract class Issue2564 {
  public enum EnumType {
    // :: error: (enum.declaration)
    @KeyFor("myMap") MY_KEY,
    // :: error: (enum.declaration)
    @KeyFor("enumMap") ENUM_KEY;
    private static final Map<String, Integer> enumMap = new HashMap<>();

    void method() {
      @KeyFor("enumMap") EnumType t = ENUM_KEY;
      int x = enumMap.get(ENUM_KEY);
    }
  }

  private static final Map<String, Integer> myMap = new HashMap<>();
}
