package simplecrash;

public class LetItCrash implements CrashyInterface {
    private Long longer = 0L;

    @Override
    public void makeItLongerAndCrash() {
        this.longer += 0;
    }
}
