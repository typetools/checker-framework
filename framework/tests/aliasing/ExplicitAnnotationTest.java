import org.checkerframework.common.aliasing.qual.Unique;

@Unique class UniqueData {
  @SuppressWarnings("unique.leaked")
  UniqueData() {} // All objects of UniqueData are now @Unique
}

public class ExplicitAnnotationTest {
  void check(UniqueData p) { // p is @Unique UniqueData Object
    // :: error: (unique.leaked)
    UniqueData y = p; // @Unique p is leaked
    // :: error: (unique.leaked)
    Object z = p; // @Unique p is leaked
  }
}
