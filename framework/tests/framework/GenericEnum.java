// Test case for issue #279: https://github.com/typetools/checker-framework/issues/279

class GenericEnum<T extends String> {

    void test() {
        T.format("");
    }
}
