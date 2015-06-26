import static java.util.Arrays.asList;
import java.util.function.Supplier;
import java.util.List;

class Issue436 {
    public void makeALongFormConditionalLambdaReturningGenerics(boolean makeAll) {
        Supplier<List<String>> supplier = () -> {
            if (makeAll) {
                return asList("beer", "peanuts");
            }
            else {
                return asList("cheese", "wine");
            }
        };
    }
}
