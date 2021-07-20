import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.checkerframework.common.returnsreceiver.qual.*;

@Builder
@Accessors(fluent = true)
public class BuilderTest {
    @Getter @Setter private Integer x;
    @Getter @Setter @NonNull private Integer y;
    @Getter @Setter @NonNull private Integer z;

    public static void test_simplePattern() {
        BuilderTest.builder().x(0).y(0).build();
        BuilderTest.builder().y(0).build();
        BuilderTest.builder().y(0).z(5).build();
    }

    public static void test_builderVar() {
        final BuilderTest.BuilderTestBuilder goodBuilder = new BuilderTestBuilder();
        goodBuilder.x(0);
        goodBuilder.y(0);
        goodBuilder.build();
    }
}

class CustomBuilderTestBuilder extends BuilderTest.BuilderTestBuilder {
    // wrapper methods to ensure @This annotations are getting added properly
    BuilderTest.@This BuilderTestBuilder wrapperX() {
        return x(0);
    }

    BuilderTest.@This BuilderTestBuilder wrapperY() {
        return y(1);
    }

    BuilderTest.@This BuilderTestBuilder wrapperZ() {
        return z(2);
    }
}
