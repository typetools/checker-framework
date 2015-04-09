abstract class Files {
  public <R extends Readable & Closeable> void copy(InputSupplier<R> from) {
    OutputSupplier<OutputStreamWriter> supplier = newWriterSupplier();
    if (supplier != null) {
      CharStreams.copy(from, supplier);
    }
  }

  public <W extends Appendable & Closeable> void copy(OutputSupplier<W> to) {
    InputSupplier<InputStreamReader> supplier = newReaderSupplier();
    if (supplier != null) {
      CharStreams.copy(supplier, to);
    }
  }

  abstract OutputSupplier<OutputStreamWriter> newWriterSupplier();
  abstract InputSupplier<InputStreamReader> newReaderSupplier();
}
