// Test case for issue #279: https://code.google.com/p/checker-framework/issues/detail?id=279

class GenericEnum<T extends String> {

    void test() {
        T.format("");
    }
}
