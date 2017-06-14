import java.util.Optional;

public class Marks4 {

    String getDefault() {
        return "Fozzy Bear";
    }

    String process_bad(String s) {
        return Optional.ofNullable(s).orElseGet(this::getDefault);
    }

    String process_good(String s) {
        return (s != null) ? s : getDefault();
    }
}
