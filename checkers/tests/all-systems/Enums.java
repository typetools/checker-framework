import java.lang.annotation.ElementType;

class MyEnumSet<E extends Enum<E>> {}

class Enumeration {
  public enum VarFlags {IS_PARAM, NO_DUPS};
  public MyEnumSet<VarFlags> var_flags = new MyEnumSet<VarFlags>();

  VarFlags f1 = VarFlags.IS_PARAM;
  
  void foo1(MyEnumSet<VarFlags> p) {}
  void foo2(MyEnumSet<ElementType> p) {}
}