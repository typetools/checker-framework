class ClientBuilder<T extends ClientBuilder<T>> {

  static ClientBuilder<?> newBuilder() {
    return new BuilderImpl();
  }

  // Dummy class to get the recursive Builder typing right.
  static class BuilderImpl extends ClientBuilder<BuilderImpl> {}

  T setThing() {
    return (T) this;
  }
}
