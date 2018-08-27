package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.BugInCF;
import org.junit.Ignore;
import org.junit.Test;
import testlib.util.AnnoWithStringArg;
import testlib.util.Encrypted;

public class AnnotationBuilderTest {

    private static BugInCF dummy = null;

    private final ProcessingEnvironment env;

    public AnnotationBuilderTest() {
        env = JavacProcessingEnvironment.instance(new Context());
    }

    @Test
    public void createAnnoWithoutValues() {
        AnnotationBuilder builder = new AnnotationBuilder(env, Encrypted.class);
        // AnnotationMirror anno =
        builder.build();
    }

    @Test
    public void createAnnoWithoutValues1() {
        AnnotationBuilder builder = new AnnotationBuilder(env, AnnoWithStringArg.class);
        AnnotationMirror anno = builder.build();
        assertEquals(0, anno.getElementValues().size());
    }

    @Test
    public void createAnnoWithValues0() {
        AnnotationBuilder builder = new AnnotationBuilder(env, AnnoWithStringArg.class);
        builder.setValue("value", "m");
        AnnotationMirror anno = builder.build();
        assertEquals(1, anno.getElementValues().size());
    }

    @Test(expected = BugInCF.class)
    public void buildingTwice() {
        AnnotationBuilder builder = new AnnotationBuilder(env, Encrypted.class);
        builder.build();
        builder.build();
    }

    @Test(expected = BugInCF.class)
    public void addingValuesAfterBuilding() {
        AnnotationBuilder builder = new AnnotationBuilder(env, AnnoWithStringArg.class);
        builder.setValue("value", "m");
        // AnnotationMirror anno =
        builder.build();
        builder.setValue("value", "n");
    }

    @Test(expected = BugInCF.class)
    public void notFoundElements() {
        AnnotationBuilder builder = new AnnotationBuilder(env, AnnoWithStringArg.class);
        builder.setValue("n", "m");
    }

    @Test(expected = BugInCF.class)
    public void illegalValue() {
        AnnotationBuilder builder = new AnnotationBuilder(env, AnnoWithStringArg.class);
        builder.setValue("value", 1);
    }

    public static @interface A {
        int[] numbers();
    }

    public static @interface B {
        String[] strings();
    }

    @Test
    public void listArrayPrimitive() {
        AnnotationBuilder builder = new AnnotationBuilder(env, A.class);
        builder.setValue("numbers", new Integer[] {34, 32, 43});
        assertEquals(1, builder.build().getElementValues().size());
    }

    @Test
    public void listArrayObject() {
        AnnotationBuilder builder = new AnnotationBuilder(env, B.class);
        builder.setValue("strings", new String[] {"m", "n"});
        assertEquals(1, builder.build().getElementValues().size());
    }

    @Test(expected = BugInCF.class)
    public void listArrayObjectWrongType() {
        AnnotationBuilder builder = new AnnotationBuilder(env, B.class);
        builder.setValue("strings", new Object[] {"m", "n", 1});
        assertEquals(1, builder.build().getElementValues().size());
    }

    @Test(expected = BugInCF.class)
    public void listArrayObjectWrongType1() {
        AnnotationBuilder builder = new AnnotationBuilder(env, B.class);
        builder.setValue("strings", 1);
        assertEquals(1, builder.build().getElementValues().size());
    }

    public static @interface Prim {
        int a();
    }

    @Test
    public void primitiveValue() {
        AnnotationBuilder builder = new AnnotationBuilder(env, Prim.class);
        builder.setValue("a", 3);
        assertEquals(1, builder.build().getElementValues().size());
    }

    @Test(expected = BugInCF.class)
    public void primitiveValueWithException() {
        AnnotationBuilder builder = new AnnotationBuilder(env, A.class);
        builder.setValue("a", 3.0);
        assertEquals(1, builder.build().getElementValues().size());
    }

    // Multiple values
    public static @interface Mult {
        int a();

        String b();
    }

    @Test
    public void multiple1() {
        AnnotationBuilder builder = new AnnotationBuilder(env, Mult.class);
        builder.setValue("a", 2);
        assertEquals(1, builder.build().getElementValues().size());
    }

    @Test(expected = BugInCF.class)
    public void multiple2() {
        AnnotationBuilder builder = new AnnotationBuilder(env, Mult.class);
        builder.setValue("a", "m");
        assertEquals(1, builder.build().getElementValues().size());
    }

    @Test
    public void multiple3() {
        AnnotationBuilder builder = new AnnotationBuilder(env, Mult.class);
        builder.setValue("a", 1);
        builder.setValue("b", "mark");
        assertEquals(2, builder.build().getElementValues().size());
    }

    public static @interface ClassElt {
        Class<?> value();
    }

    @Test
    public void testClassPositive() {
        AnnotationBuilder builder = new AnnotationBuilder(env, ClassElt.class);
        builder.setValue("value", String.class);
        builder.setValue("value", int.class);
        builder.setValue("value", int[].class);
        builder.setValue("value", void.class);
        Object storedValue =
                builder.build().getElementValues().values().iterator().next().getValue();
        assertTrue("storedValue is " + storedValue.getClass(), storedValue instanceof TypeMirror);
    }

    @Test(expected = BugInCF.class)
    public void testClassNegative() {
        AnnotationBuilder builder = new AnnotationBuilder(env, ClassElt.class);
        builder.setValue("value", 2);
    }

    public static @interface RestrictedClassElt {
        Class<? extends Number> value();
    }

    @Test
    public void testRestClassPositive() {
        AnnotationBuilder builder = new AnnotationBuilder(env, RestrictedClassElt.class);
        builder.setValue("value", Integer.class);
    }

    // Failing test for now.  AnnotationBuilder is a bit permissive
    // It doesn't not check type argument subtyping
    @Test(expected = BugInCF.class)
    @Ignore // bug for now
    public void testRetClassNegative() {
        AnnotationBuilder builder = new AnnotationBuilder(env, RestrictedClassElt.class);
        builder.setValue("value", String.class);
    }

    enum MyEnum {
        OK,
        NOT;
    }

    enum OtherEnum {
        TEST;
    }

    public static @interface EnumElt {
        MyEnum value();
    }

    @Test
    public void testEnumPositive() {
        AnnotationBuilder builder = new AnnotationBuilder(env, EnumElt.class);
        builder.setValue("value", MyEnum.OK);
        builder.setValue("value", MyEnum.NOT);
    }

    @Test(expected = BugInCF.class)
    public void testEnumNegative() {
        AnnotationBuilder builder = new AnnotationBuilder(env, EnumElt.class);
        builder.setValue("value", 2);
    }

    @Test(expected = BugInCF.class)
    public void testEnumNegative2() {
        AnnotationBuilder builder = new AnnotationBuilder(env, EnumElt.class);
        builder.setValue("value", OtherEnum.TEST);
    }

    public static @interface Anno {
        String value();

        int[] can();
    }

    @Test
    public void testToString1() {
        AnnotationBuilder builder = new AnnotationBuilder(env, Anno.class);
        assertEquals("@tests.AnnotationBuilderTest.Anno", builder.build().toString());
    }

    @Test
    public void testToString2() {
        AnnotationBuilder builder = new AnnotationBuilder(env, Anno.class);
        builder.setValue("value", "string");
        assertEquals("@tests.AnnotationBuilderTest.Anno(\"string\")", builder.build().toString());
    }

    @Test
    public void testToString3() {
        AnnotationBuilder builder = new AnnotationBuilder(env, Anno.class);
        builder.setValue("can", new Object[] {1});
        assertEquals("@tests.AnnotationBuilderTest.Anno(can={1})", builder.build().toString());
    }

    @Test
    public void testToString4() {
        AnnotationBuilder builder = new AnnotationBuilder(env, Anno.class);
        builder.setValue("value", "m");
        builder.setValue("can", new Object[] {1});
        assertEquals(
                "@tests.AnnotationBuilderTest.Anno(value=\"m\", can={1})",
                builder.build().toString());
    }

    @Test
    public void testToString5() {
        AnnotationBuilder builder = new AnnotationBuilder(env, Anno.class);
        builder.setValue("can", new Object[] {1});
        builder.setValue("value", "m");
        assertEquals(
                "@tests.AnnotationBuilderTest.Anno(can={1}, value=\"m\")",
                builder.build().toString());
    }

    public static @interface MyAnno {}

    public static @interface ContainingAnno {
        MyAnno value();
    }

    @Test
    public void testAnnoAsArgPositive() {
        AnnotationMirror anno = AnnotationBuilder.fromClass(env.getElementUtils(), MyAnno.class);
        AnnotationBuilder builder = new AnnotationBuilder(env, ContainingAnno.class);
        builder.setValue("value", anno);
        assertEquals(
                "@tests.AnnotationBuilderTest.ContainingAnno(@tests.AnnotationBuilderTest.MyAnno)",
                builder.build().toString());
    }

    @Test(expected = BugInCF.class)
    public void testAnnoAsArgNegative() {
        AnnotationMirror anno = AnnotationBuilder.fromClass(env.getElementUtils(), Anno.class);
        AnnotationBuilder builder = new AnnotationBuilder(env, ContainingAnno.class);
        builder.setValue("value", anno);
    }
}
