public class InternUnbox {
    void method() {
        Boolean leftBoolean = getBooleanValue();
        createBooleanCFValue(!leftBoolean);
    }

    private void createBooleanCFValue(boolean b) {}

    private Boolean getBooleanValue() {
        return Boolean.FALSE;
    }
}
