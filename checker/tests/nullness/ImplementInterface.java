import org.checkerframework.checker.nullness.qual.*;

interface TestInterface {
    public char @Nullable [] getChars();
}

class ImplementInterface implements TestInterface {
    @Override
    public char @Nullable [] getChars() {
        return null;
    }
}
