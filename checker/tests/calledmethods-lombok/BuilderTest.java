import lombok.Builder;
import lombok.NonNull;

@Builder
public class BuilderTest {
  private Integer x;
  @NonNull private Integer y;
  @Builder.Default @NonNull private Integer z = 5;

  public static void test_simplePattern() {
    BuilderTest.builder().x(0).y(0).build(); // good builder
    BuilderTest.builder().y(0).build(); // good builder
    BuilderTest.builder().y(0).z(5).build(); // good builder
    // :: error: (finalizer.invocation.invalid)
    BuilderTest.builder().x(0).build(); // bad builder
  }

  public static void test_builderVar() {
    final BuilderTest.BuilderTestBuilder goodBuilder = new BuilderTestBuilder();
    goodBuilder.x(0);
    goodBuilder.y(0);
    goodBuilder.build();
    final BuilderTest.BuilderTestBuilder badBuilder = new BuilderTestBuilder();
    // :: error: (finalizer.invocation.invalid)
    badBuilder.build();
  }
}
