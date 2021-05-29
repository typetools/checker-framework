public class Autoboxing {
  Byte b;
  Short s;
  Short sInterned;
  Integer i;
  Integer iInterned;
  Long l;
  Float f;
  Double d;
  Boolean z;
  Character c;
  Character cInterned;

  Autoboxing() {
    b = -126;
    s = 32000;
    sInterned = 32;
    i = 1234567;
    iInterned = 123;
    l = 1234567L;
    f = 3.14f;
    d = 3.14;
    z = true;
    c = 65000;
    cInterned = 65;
  }

  public static void main(String[] args) {
    new Autoboxing().test();
  }

  public void test() {
    System.out.println();
    System.out.println("Byte");
    Byte b1 = -126;
    Byte b2 = -126;
    Byte b3 = Byte.valueOf((byte) -126);
    System.out.println(b1 == b2);
    // :: warning: (unnecessary.equals)
    System.out.println(b1.equals(b2));
    // :: warning: (unnecessary.equals)
    System.out.println(b3.equals(b2));
    System.out.println(b.equals(b2));
    System.out.println(b == -126);
    // :: warning: (unnecessary.equals)
    System.out.println(b1.equals(126));

    System.out.println();
    System.out.println("Short");
    Short s1 = 32000;
    Short s2 = 32000;
    Short s3 = Short.valueOf((short) 32000);
    // :: error: (not.interned)
    System.out.println(s1 == s2);
    System.out.println(s1.equals(s2));
    System.out.println(s3.equals(s2));
    System.out.println(s.equals(s2));
    // TODO
    //     Short s1interned = 32;
    //     Short s2interned = 32;
    //     Short s3interned = Short.valueOf((short) 32);
    //     System.out.println(s1interned==s2interned);
    //     // :: warning: (unnecessary.equals)
    //     System.out.println(s1interned.equals(s2interned));
    //     // :: warning: (unnecessary.equals)
    //     System.out.println(s3interned.equals(s2interned));
    //     // :: warning: (unnecessary.equals)
    //     System.out.println(sInterned.equals(s2interned));

    System.out.println();
    System.out.println("Integer");
    Integer i1 = 1234567;
    Integer i2 = 1234567;
    Integer i3 = Integer.valueOf(1234567);
    // :: error: (not.interned)
    System.out.println(i1 == i2);
    System.out.println(i1.equals(i2));
    System.out.println(i3.equals(i2));
    System.out.println(i.equals(i2));

    System.out.println();
    Integer i1interned = 123;
    Integer i2interned = 123;
    Integer i3interned = Integer.valueOf(123);
    // TODO:
    // Would be legal to use ==, but Interning Checker does not check the
    // actual int value when deciding whether to warn for unnecessary.equals.
    //     // :: warning: (unnecessary.equals)
    //     System.out.println(i1interned==i2interned);
    //     // :: warning: (unnecessary.equals)
    //     System.out.println(i1interned.equals(i2interned));
    //     // :: warning: (unnecessary.equals)
    //     System.out.println(i3interned.equals(i2interned));
    //     // :: warning: (unnecessary.equals)
    //     System.out.println(iInterned.equals(i2interned));
    //     System.out.println(i1interned==123); // ok
    //     // :: warning: (unnecessary.equals)
    //     System.out.println(i1interned.equals(123));

    System.out.println();
    System.out.println("Long");
    Long l1 = 1234567L;
    Long l2 = 1234567L;
    Long l3 = Long.valueOf(1234567L);
    // :: error: (not.interned)
    System.out.println(l1 == l2);
    System.out.println(l1.equals(l2));
    System.out.println(l3.equals(l2));
    System.out.println(l.equals(l2));

    System.out.println();
    System.out.println("Float");
    Float f1 = 3.14f;
    Float f2 = 3.14f;
    Float f3 = Float.valueOf(3.14f);
    // :: error: (not.interned)
    System.out.println(f1 == f2);
    System.out.println(f1.equals(f2));
    System.out.println(f3.equals(f2));
    System.out.println(f.equals(f2));

    System.out.println();
    System.out.println("Double");
    Double d1 = 3.14;
    Double d2 = 3.14;
    Double d3 = Double.valueOf(3.14);
    // :: error: (not.interned)
    System.out.println(d1 == d2);
    System.out.println(d1.equals(d2));
    System.out.println(d3.equals(d2));
    System.out.println(d.equals(d2));

    System.out.println();
    System.out.println("Boolean");
    Boolean z1 = true;
    Boolean z2 = true;
    Boolean z3 = Boolean.valueOf(true);
    System.out.println(z1 == z2);
    // :: warning: (unnecessary.equals)
    System.out.println(z1.equals(z2));
    // :: warning: (unnecessary.equals)
    System.out.println(z3.equals(z2));
    System.out.println(z.equals(z2));
    System.out.println(z1 == true); // ok
    // :: warning: (unnecessary.equals)
    System.out.println(z1.equals(true));

    System.out.println();
    System.out.println("Character");
    Character c1 = 65000;
    Character c2 = 65000;
    Character c3 = Character.valueOf((char) 65000);
    // :: error: (not.interned)
    System.out.println(c1 == c2);
    System.out.println(c1.equals(c2));
    System.out.println(c3.equals(c2));
    System.out.println(c.equals(c2));
    // TODO
    //     Character c1interned = 65;
    //     Character c2interned = 65;
    //     Character c3interned = Character.valueOf((char) 65);
    //     System.out.println(c1interned==c2interned);
    //     // :: warning: (unnecessary.equals)
    //     System.out.println(c1interned.equals(c2interned));
    //     // :: warning: (unnecessary.equals)
    //     System.out.println(c3interned.equals(c2interned));
    //     // :: warning: (unnecessary.equals)
    //     System.out.println(cInterned.equals(c2interned));
  }
}
