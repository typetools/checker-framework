import lombok.Builder;

@Builder
public class DefaultedName {
    @Builder.Default @lombok.NonNull String name = "Martin";

    static void test1() {
        builder().build();
    }

    static void test2(Object foo) {
        DefaultedNameBuilder b = builder();
        if (foo != null) {
            b.name(foo.toString());
        }
        b.build();
    }
}
