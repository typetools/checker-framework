import java.util.List;
import lombok.Builder;
import lombok.Singular;

@Builder
public class LombokSingularExample {
    @Singular @lombok.NonNull List<Object> items;

    // This one should be permitted, because @Singular will
    // produce an empty list even if one is not specified directly.
    public static void testNoItems() {
        LombokSingularExample.builder().build();
    }

    // This call should also be permitted, even though items() is
    // not called, because the list will be automatically initialized.
    public static void testOneItem() {
        LombokSingularExample.builder().item(new Object()).build();
    }

    public static void testWithList(List<Object> l) {
        LombokSingularExample.builder().items(l).build();
    }
}
