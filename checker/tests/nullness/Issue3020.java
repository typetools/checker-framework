enum Issue3020 {
  INSTANCE;

  void retrieveConstant() {
    Class<?> theClass = Issue3020.class;
    // :: error: (accessing.nullable)
    Object unused = passThrough(theClass.getEnumConstants())[0];
  }

  void nonNullArray(String[] p) {
    Object unused = passThrough(p)[0];
  }

  <T> T passThrough(T t) {
    return t;
  }
}
