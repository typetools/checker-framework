import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

@Builder
@Accessors(fluent = true)
public class BuilderTest {
    @Getter @Setter private Integer x;
    @Getter @Setter @NonNull private Integer y;
    @Getter @Setter @NonNull private Integer z;

    public static void test_simplePattern() {
        BuilderTest.builder().x(0).y(0).build(); // good builder
        BuilderTest.builder().y(0).build(); // good builder
        BuilderTest.builder().y(0).z(5).build(); // good builder
    }

    public static void test_builderVar() {
        final BuilderTest.BuilderTestBuilder goodBuilder = new BuilderTestBuilder();
        goodBuilder.x(0);
        goodBuilder.y(0);
        goodBuilder.build();
    }
}
