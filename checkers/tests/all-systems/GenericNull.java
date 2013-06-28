
class GenericNull {
    /**
     * In most type systems, null's type will be bottom and therefore the
     * generic return type T will be a supertype of null's type.
     * However, in nullness type systems, null's type is not bottom, so
     * they exclude this test.
     */
    @SuppressWarnings("nullness")
    <T> T f() {
        return null;
    }
}
