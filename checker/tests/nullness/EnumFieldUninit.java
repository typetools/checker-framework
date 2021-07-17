enum EnumFieldUninit {
  DUMMY;
  //  // :: error: (assignment)
  //  public static String s = null;
  // :: error: (initialization.static.field.uninitialized)
  public static String u;
}
