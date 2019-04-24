import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

class Issue2407 {

    @NonNull String message = "default string";

    @RequiresNonNull("#1")
    void setMessage(String message) {
        this.message = message;
    }

    String getMessage() {
        return message;
    }

    void main() {
        Issue2407 object = new Issue2407();
        // :: error: (contracts.precondition.not.satisfied)
        object.setMessage(object.getMessage() + "extra");
    }
}
