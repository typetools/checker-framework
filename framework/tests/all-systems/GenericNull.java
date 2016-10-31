
class GenericNull {
    /**
     * In most type systems, null's type will be bottom and therefore the
     * generic return type T will be a supertype of null's type.
     * However, in nullness and lock type systems, null's type is not bottom, so
     * they exclude this test.
     * For the Lock Checker, null's type is bottom for the @GuardedByUnknown
     * hierarchy but not for the @LockPossiblyHeld hierarchy.
     */
    @SuppressWarnings({"nullness", "lock"})
    <T> T f() {
        return null;
    }
}
