// Test annotation on Enum.name()

// Test case for Issue 1402:
// https://github.com/typetools/checker-framework/issues/1402

public enum Issue1402EnumName {
    TEST_ONE("abc"),
    TEST_TWO("def");

    private final String newName;

    Issue1402EnumName(String customData) {
        this.newName = name();
    }
}
