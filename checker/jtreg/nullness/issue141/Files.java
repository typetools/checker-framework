abstract class Files {
  public <R extends Readable & Closeable> void copy(InputSupplier<R> from) {
    CharStreams.copy(from, newWriterSupplier());
  }

  public <W extends Appendable & Closeable> void copy(OutputSupplier<W> to) {
    CharStreams.copy(newReaderSupplier(), to);
  }

  abstract OutputSupplier<OutputStreamWriter> newWriterSupplier();

  abstract InputSupplier<InputStreamReader> newReaderSupplier();
}
