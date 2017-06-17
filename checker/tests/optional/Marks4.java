import java.util.Optional;

// "It’s generally a bad idea to create an Optional for the specific purpose of chaining methods
// from it to get a value."
// Optional creation: of, ofNullable.
// Things that eliminate Optional: get, orElse, orElseGet, orElseThrow.

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
