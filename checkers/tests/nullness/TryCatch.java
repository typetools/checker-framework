import checkers.nullness.quals.*;

class TryCatch {
    void t(String[] xs) {
        String t = "";
        t.toString();
        try {
        } catch (Throwable e) {
            t.toString();
        }
    }
}
