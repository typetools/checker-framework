import java.util.Optional;

/** Test case for flow-sensitivity of Optional.isPresent(). */
@SuppressWarnings("optional.parameter")
public class FlowSensitivity {

    String noCheck(Optional<String> opt) {
        // :: error: (method.invocation.invalid)
        return opt.get();
    }

    String hasCheck1(Optional<String> opt) {
        if (opt.isPresent()) {
            return opt.get();
        } else {
            return "default";
        }
    }

    String hasCheck2(Optional<String> opt) {
        if (!opt.isPresent()) {
            return "default";
        }
        return opt.get();
    }
}
