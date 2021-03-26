public class CharStreams {
  static <R extends Readable & Closeable, W extends Appendable & Closeable> void copy(
      InputSupplier<R> from, OutputSupplier<W> to) {}
}
