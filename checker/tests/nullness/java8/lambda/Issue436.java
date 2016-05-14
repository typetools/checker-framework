import static java.util.Arrays.asList;
import java.util.function.Supplier;
import java.util.List;

//@below-java8-jdk-skip-test
class Issue436 {
    public void makeALongFormConditionalLambdaReturningGenerics(boolean makeAll) {
        // TypeArgInferenceUtil.assignedTo used to try to use the method return rather than the lambda return
        // for those return statements below
        Supplier<List<String>> supplier = () -> {
            if (makeAll) {
                return asList("beer", "peanuts");
            } else {
                return asList("cheese", "wine");
            }
        };
    }
}
