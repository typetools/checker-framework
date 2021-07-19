import static java.util.Arrays.asList;

import java.util.List;
import java.util.function.Supplier;

public class Issue436 {
    public void makeALongFormConditionalLambdaReturningGenerics(boolean makeAll) {
        // TypeArgInferenceUtil.assignedTo used to try to use the method return rather than the
        // lambda return for those return statements below
        Supplier<List<String>> supplier =
                () -> {
                    if (makeAll) {
                        return asList("beer", "peanuts");
                    } else {
                        return asList("cheese", "wine");
                    }
                };
    }
}
