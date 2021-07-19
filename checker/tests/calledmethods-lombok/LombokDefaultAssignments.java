import java.util.Optional;
import lombok.Builder;

@Builder
public class LombokDefaultAssignments {
    @lombok.NonNull Optional<String> bar;

    public static class LombokDefaultAssignmentsBuilder {
        private Optional<String> bar = Optional.empty();
    }

    static void test() {
        LombokDefaultAssignments.builder().build();
    }
}
