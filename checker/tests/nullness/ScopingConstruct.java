import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("initialization.fields.uninitialized")
class ScopingConstruct {

    // TODO: add nested classes within these two?
    static class StaticNested {}

    class Inner {}

    @Nullable StaticNested nsn;

    @Nullable Inner ni;

    // This is a Java error.
    // @Nullable ScopingConstruct.StaticNested nscsn;

    ScopingConstruct.@Nullable StaticNested scnsn;

    // :: error: (nullness.on.outer)
    @Nullable ScopingConstruct.Inner nsci;

    ScopingConstruct.@Nullable Inner scni;
}
