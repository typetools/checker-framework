import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.checkerframework.common.returnsreceiver.qual.*;

@Builder
@Accessors(fluent = true)
public class BuilderMethodRef {
  @Getter @Setter @lombok.NonNull String foo;
  @Getter @Setter Object bar;

  public static void test(Optional<Object> opt) {
    BuilderMethodRefBuilder b = builder().foo("Hello");
    opt.ifPresent(b::bar);
    b.build();
  }
}

class CustomBuilderMethodRefBuilder extends BuilderMethodRef.BuilderMethodRefBuilder {
  // wrapper methods to ensure @This annotations are getting added properly
  BuilderMethodRef.@This BuilderMethodRefBuilder wrapperFoo() {
    return foo("dummy");
  }

  BuilderMethodRef.@This BuilderMethodRefBuilder wrapperBar() {
    return bar(new Object());
  }
}
