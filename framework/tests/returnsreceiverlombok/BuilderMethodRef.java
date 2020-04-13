import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

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
