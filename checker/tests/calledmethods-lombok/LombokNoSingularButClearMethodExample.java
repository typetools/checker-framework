import java.util.List;
import lombok.Builder;

@Builder
public class LombokNoSingularButClearMethodExample {
  @lombok.NonNull List<Object> items;

  // This one should throw an error, because the field isn't
  // automatically initialized.
  public static void testNoItems() {
    // :: error: finalizer.invocation.invalid
    LombokNoSingularButClearMethodExample.builder().build();
  }

  public static void testWithList(List<Object> l) {
    LombokNoSingularButClearMethodExample.builder().items(l).build();
  }

  public static class LombokNoSingularButClearMethodExampleBuilder {
    public void clearItems() {}
  }
}
