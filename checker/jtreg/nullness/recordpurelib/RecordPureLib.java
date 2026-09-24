import org.checkerframework.checker.nullness.qual.Nullable;

// RecordPureFromBytecode compiles this record, then type-checks clients against its bytecode.
record RecordPureLib(@Nullable String generated, @Nullable String explicit) {
  // Not @Pure, so it must not be treated as pure.
  public @Nullable String explicit() {
    return explicit;
  }
}
