package org.checkerframework.afu.scenelib.test.classfile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.StringWriter;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.AnnotationBuilder;
import org.checkerframework.afu.scenelib.AnnotationFactory;
import org.checkerframework.afu.scenelib.Annotations;
import org.checkerframework.afu.scenelib.ArrayBuilder;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.AElement;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.afu.scenelib.el.DefCollector;
import org.checkerframework.afu.scenelib.el.DefException;
import org.checkerframework.afu.scenelib.el.LocalLocation;
import org.checkerframework.afu.scenelib.el.TypeASTMapper;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.afu.scenelib.field.AnnotationAFT;
import org.checkerframework.afu.scenelib.field.ArrayAFT;
import org.checkerframework.afu.scenelib.field.BasicAFT;
import org.checkerframework.afu.scenelib.field.ClassTokenAFT;
import org.checkerframework.afu.scenelib.field.EnumAFT;
import org.checkerframework.afu.scenelib.io.IndexFileParser;
import org.checkerframework.afu.scenelib.io.IndexFileWriter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.TypePath;
import org.plumelib.util.FileIOException;

public class TestSceneLib {
  LineNumberReader openPackagedIndexFile(String name) {
    if (name == null) {
      throw new RuntimeException("Name is null");
    }
    InputStream inputStream = TestSceneLib.class.getResourceAsStream(name);
    if (inputStream == null) {
      throw new RuntimeException("Can't find resource " + name);
    }
    return new LineNumberReader(
        new InputStreamReader(
            TestSceneLib.class.getResourceAsStream(name), StandardCharsets.UTF_8));
  }

  static final String fooIndexContents =
      "package:\n"
          + "annotation @Ready: @Retention(RUNTIME)\n"
          + "annotation @Author: @Retention(CLASS)\n"
          + "String value\n"
          + "class Foo:\n"
          + "field x: @Ready\n"
          + "method y()Z:\n"
          + "parameter #5:\n"
          + "type:\n"
          + "inner-type 0, 0, 3, 2:\n"
          + "@Author(value=\"Matt M.\")\n";

  public static AnnotationDef adAuthor =
      Annotations.createValueAnnotationDef(
          "Author", Annotations.asRetentionClass, BasicAFT.forType(String.class), "TestSceneLib");

  static final AnnotationDef ready =
      new AnnotationDef(
          "Ready", Annotations.asRetentionRuntime, Annotations.noFieldTypes, "TestSceneLib.java");
  static final AnnotationDef readyClassRetention =
      new AnnotationDef(
          "Ready", Annotations.asRetentionClass, Annotations.noFieldTypes, "TestSceneLib.java");

  /**
   * Parse indexFileContents as an annotation file, merging the results into s; the final state of s
   * should equal expectScene.
   */
  void doParseTest(String indexFileContents, String indexFileName, AScene s, AScene expectScene) {
    try {
      IndexFileParser.parseString(indexFileContents, indexFileName, s);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (!expectScene.equals(s)) {
      System.err.println("expectScene does not equal s");
      String esu = expectScene.unparse();
      String su = s.unparse();
      if (esu.equals(su)) {
        System.err.println("(but their printed representations are the same)");
      }
      System.err.println(esu);
      System.err.println(su);
    }
    Assert.assertEquals(expectScene, s);
  }

  // lazy typist!
  AScene newScene() {
    return new AScene();
  }

  void doParseTest(String index, String filename, @NonNull AScene expectScene) {
    AScene s = newScene();
    doParseTest(index, filename, s, expectScene);
  }

  private Annotation createEmptyAnnotation(AnnotationDef def) {
    return new Annotation(def, Collections.<String, Object>emptyMap());
  }

  @Test
  public void testEquals() {
    AScene s1 = newScene(), s2 = newScene();

    s1.classes.getVivify("Foo");
    s1.classes.getVivify("Foo").fields.getVivify("x");
    s1.classes
        .getVivify("Foo")
        .fields
        .getVivify("x")
        .tlAnnotationsHere
        .add(createEmptyAnnotation(ready));

    s2.classes.getVivify("Foo").fields.getVivify("x").tlAnnotationsHere.add(Annotations.aNonNull);
    s2.classes
        .getVivify("Foo")
        .fields
        .getVivify("x")
        .tlAnnotationsHere
        .add(createEmptyAnnotation(ready));

    Assert.assertEquals(false, s1.equals(s2)); // FIXME: why does assertion fail?

    s1.classes.getVivify("Foo").fields.getVivify("x").tlAnnotationsHere.add(Annotations.aNonNull);

    Assert.assertEquals(true, s1.equals(s2));
  }

  @Test
  public void testStoreParse1() {
    AScene s1 = newScene();

    s1.classes
        .getVivify("Foo")
        .fields
        .getVivify("x")
        .tlAnnotationsHere
        .add(createEmptyAnnotation(ready));
    Annotation myAuthor = Annotations.createValueAnnotation(adAuthor, "Matt M.");
    s1.classes.getVivify("Foo");
    s1.classes.getVivify("Foo").methods.getVivify("y()Z");
    s1.classes.getVivify("Foo").methods.getVivify("y()Z").parameters.getVivify(5);
    @SuppressWarnings("unused")
    Object dummy =
        s1.classes.getVivify("Foo").methods.getVivify("y()Z").parameters.getVivify(5).type;
    @SuppressWarnings("unused")
    Object dummy2 =
        s1.classes
            .getVivify("Foo")
            .methods
            .getVivify("y()Z")
            .parameters
            .getVivify(5)
            .type
            .innerTypes;
    s1.classes
        .getVivify("Foo")
        .methods
        .getVivify("y()Z")
        .parameters
        .getVivify(5)
        .type
        .innerTypes
        .getVivify(TypePathEntry.getTypePathEntryListFromBinary(Arrays.asList(0, 0, 3, 2)))
        .tlAnnotationsHere
        .add(myAuthor);

    doParseTest(fooIndexContents, "fooIndexContents", s1);
  }

  private void checkConstructor(AMethod constructor) {
    Annotation ann = constructor.receiver.type.lookup("p2.D");
    Assert.assertEquals(Collections.singletonMap("value", "spam"), ann.fieldValues);
    Set<Entry<LocalLocation, AField>> set = constructor.body.locals.entrySet();
    Entry<LocalLocation, AField> entry = set.iterator().next();
    Assert.assertEquals(1, entry.getKey().index[0]);
    Assert.assertEquals(3, entry.getKey().getScopeStart());
    Assert.assertEquals(5, entry.getKey().getScopeLength());
    ATypeElement l = entry.getValue().type;
    AElement i =
        l.innerTypes.get(TypePathEntry.getTypePathEntryListFromBinary(Arrays.asList(0, 0)));
    Assert.assertNotNull(i.lookup("p2.C"));
    AField l2 = constructor.body.locals.get(new LocalLocation(3, 6, 1));
    Assert.assertNull(l2);
  }

  @Test
  public void testParseRetrieve1() throws Exception {
    LineNumberReader fr = openPackagedIndexFile("test1.jaif");
    AScene s1 = newScene();
    IndexFileParser.parse(fr, "test1.jaif", s1);

    AClass foo = s1.classes.get("p1.Foo");
    Assert.assertNotNull("Didn't find foo1", foo);
    boolean sawConstructor = false;
    for (Map.Entry<String, AMethod> me : foo.methods.entrySet()) {
      if (me.getKey().equals("<init>(Ljava/util/Set;)V")) {
        Assert.assertFalse(sawConstructor);
        AMethod constructor = me.getValue();
        Assert.assertNotNull(constructor);
        checkConstructor(constructor);
        sawConstructor = true;
      }
    }
    Assert.assertTrue(sawConstructor);
  }

  static class TestDefCollector extends DefCollector {
    AnnotationDef a, b, c, d, e;

    AnnotationDef f;

    public TestDefCollector(AScene s) throws DefException {
      super(s);
    }

    @Override
    protected void visitAnnotationDef(AnnotationDef tldef) {
      if (tldef.name.equals("p2.A")) {
        Assert.assertNull(a);
        a = tldef;
      } else if (tldef.name.equals("p2.B")) {
        Assert.assertNull(b);
        b = tldef;
      } else if (tldef.name.equals("p2.C")) {
        Assert.assertNull(c);
        c = tldef;
      } else if (tldef.name.equals("p2.D")) {
        Assert.assertNull(d);
        d = tldef;
      } else if (tldef.name.equals("p2.E")) {
        Assert.assertNotNull(f); // should give us fields first
        Assert.assertNull(e);
        e = tldef;
      } else if (tldef.name.equals("p2.F")) {
        f = tldef;
      } else {
        Assert.fail();
      }
    }
  }

  @Test
  public void testParseRetrieveTypes() throws Exception {
    LineNumberReader fr = openPackagedIndexFile("test1.jaif");
    AScene s1 = newScene();
    IndexFileParser.parse(fr, "test1.jaif", s1);

    TestDefCollector tdc = new TestDefCollector(s1);
    tdc.visit();
    Assert.assertNotNull(tdc.a);
    Assert.assertNotNull(tdc.b);
    Assert.assertNotNull(tdc.c);
    Assert.assertNotNull(tdc.d);
    Assert.assertNotNull(tdc.e);
    Assert.assertNotNull(tdc.f);

    // now look at p2.E because it has some rather complex types
    AnnotationDef tle = tdc.e;
    Assert.assertEquals(RetentionPolicy.CLASS, tle.retention());
    AnnotationDef e = tle;
    Assert.assertEquals(new ArrayAFT(new AnnotationAFT(tdc.a)), e.fieldTypes.get("first"));
    Assert.assertEquals(new AnnotationAFT(tdc.f), e.fieldTypes.get("second"));
    Assert.assertEquals(new EnumAFT("Foo"), e.fieldTypes.get("third"));
    Assert.assertEquals(ClassTokenAFT.ctaft, e.fieldTypes.get("fourth"));
    Assert.assertEquals(ClassTokenAFT.ctaft, e.fieldTypes.get("fifth"));

    AnnotationDef tla = tdc.a;
    Assert.assertEquals(RetentionPolicy.RUNTIME, tla.retention());
    AnnotationDef a = tla;
    Assert.assertEquals(BasicAFT.forType(int.class), a.fieldTypes.get("value"));
    AnnotationDef d = tdc.d;
    Assert.assertEquals(BasicAFT.forType(String.class), d.fieldTypes.get("value"));
  }

  @Test
  public void testParseRetrieveValues() throws Exception {
    LineNumberReader fr = openPackagedIndexFile("test1.jaif");
    AScene s1 = newScene();
    IndexFileParser.parse(fr, "test1.jaif", s1);

    // now look at Bar because it has some rather complex values
    Annotation a = s1.classes.get("p1.Bar").lookup("p2.E");

    Assert.assertEquals("fooconstant", a.fieldValues.get("third"));
    Assert.assertEquals("interface java.util.Map", a.fieldValues.get("fourth").toString());
    Assert.assertEquals("class [[I", a.fieldValues.get("fifth").toString());

    List<?> first = (List<?>) a.fieldValues.get("first");
    Assert.assertEquals(2, first.size(), 2);
    Annotation aa = (Annotation) first.get(0);
    Assert.assertEquals("p2.A", aa.def().name);
    Assert.assertEquals(-1, aa.fieldValues.get("value"));

    Annotation a2 = s1.classes.get("p1.Baz").lookup("p2.E");
    Assert.assertEquals("FOO_FOO", a2.fieldValues.get("third"));
    Assert.assertEquals("class java.util.LinkedHashMap", a2.fieldValues.get("fourth").toString());
    Assert.assertEquals("void", a2.fieldValues.get("fifth").toString());
  }

  void doRewriteTest(LineNumberReader r, String filename) throws Exception {
    AScene s1 = newScene(), s2 = newScene();
    IndexFileParser.parse(r, filename, s1);
    StringWriter sbw = new StringWriter();
    IndexFileWriter.write(s1, sbw);
    IndexFileParser.parseString(sbw.toString(), "unparsed " + filename, s2);
    Assert.assertEquals(s1, s2);
  }

  @Test
  public void testRewriteOne() throws Exception {
    LineNumberReader fr = openPackagedIndexFile("test1.jaif");
    doRewriteTest(fr, "test1.jaif");
  }

  @Test
  public void testRewriteTwo() throws Exception {
    LineNumberReader fr = openPackagedIndexFile("test2.jaif");
    doRewriteTest(fr, "test2.jaif");
  }

  @Test
  public void testConflictedDefinition() throws Exception {
    AScene s1 = newScene();
    s1.classes.getVivify("Foo").tlAnnotationsHere.add(createEmptyAnnotation(ready));
    s1.classes.getVivify("Bar").tlAnnotationsHere.add(createEmptyAnnotation(readyClassRetention));
    StringWriter sbw = new StringWriter();
    try {
      IndexFileWriter.write(s1, sbw);
      Assert.fail("an exception should have been thrown");
    } catch (DefException de) {
      Assert.assertEquals("Ready", de.annotationType);
      // success
    }
  }

  @Test
  public void testParseErrorMissingColon() throws Exception {
    AScene s1 = newScene();
    String fileContents = "package p1:\n" + "annotation @A:\n" + "class Foo @A";
    try {
      IndexFileParser.parseString(fileContents, "testParseErrorMissingColon()", s1);
      Assert.fail(); // an exception should have been thrown
    } catch (FileIOException e) {
      // TODO:  check line number
      // Assert.assertEquals(3, e.line);
      // success
    }
  }

  @Test
  public void testParseErrorMissingDefinition() throws Exception {
    AScene s1 = newScene();
    String fileContents =
        "package p1:\n"
            + "annotation @AIsDefined:\n"
            + "class Foo:\n"
            + "@AIsDefined\n"
            + "@BIsNotDefined\n";
    try {
      IndexFileParser.parseString(fileContents, "testParseErrorMissingDefinition()", s1);
      Assert.fail(); // an exception should have been thrown
    } catch (FileIOException e) {
      // TODO: check line number
      // Assert.assertEquals(5, e.line);
      // success
    }
  }

  private static Annotation getAnnotation(Set<Annotation> annos, String name) {
    for (Annotation anno : annos) {
      if (anno.def.name.equals(name)) {
        return anno;
      }
    }
    return null;
  }

  @Test
  public void testEmptyArrayHack() throws Exception {
    AScene scene = newScene();
    AClass clazz = scene.classes.getVivify("bar.Test");

    // One annotation with an empty array of unknown type...
    AnnotationBuilder ab1 =
        AnnotationFactory.saf.beginAnnotation(
            "foo.ArrayAnno", Annotations.asRetentionClass, "testSceneLib");
    ab1.addEmptyArrayField("array");
    Annotation a1 = ab1.finish();
    Annotation tla1 = a1;

    // ... and another with an empty array of known type
    AnnotationBuilder ab2 =
        AnnotationFactory.saf.beginAnnotation(
            "foo.ArrayAnno", Annotations.asRetentionClass, "TestSceneLib");
    ArrayBuilder ab2ab = ab2.beginArrayField("array", new ArrayAFT(BasicAFT.forType(int.class)));
    ab2ab.finish();
    Annotation a2 = ab2.finish();
    Annotation tla2 = a2;

    // And they're both fields of another annotation to make sure that
    // unification works recursively.
    AnnotationBuilder ab3 =
        AnnotationFactory.saf.beginAnnotation(
            "foo.CombinedAnno", Annotations.asRetentionRuntime, "testSceneLib");
    ab3.addScalarField("fieldOne", new AnnotationAFT(a1.def()), a1);
    ab3.addScalarField("fieldTwo", new AnnotationAFT(a2.def()), a2);
    Annotation a3 = ab3.finish();
    Annotation tla3 = a3;

    clazz.tlAnnotationsHere.add(tla3);

    StringWriter sw = new StringWriter();
    IndexFileWriter.write(scene, sw);

    AScene sceneRead = newScene();
    IndexFileParser.parseString(sw.toString(), "testEmptyArrayHack()", sceneRead);

    // the anomaly: see second "consequence" on IndexFileWriter#write
    Assert.assertFalse(scene.equals(sceneRead));

    AClass clazz2 = sceneRead.classes.get("bar.Test");
    Assert.assertNotNull(clazz2);
    Annotation a3_2 = getAnnotation(clazz2.tlAnnotationsHere, "foo.CombinedAnno");
    Annotation a1_2 = (Annotation) a3_2.getFieldValue("fieldOne");
    Annotation a2_2 = (Annotation) a3_2.getFieldValue("fieldTwo");
    // now that the defs were merged, the annotations should be equal
    Assert.assertEquals(a1_2, a2_2);

    // Yet another annotation with an array of a different known type
    AnnotationBuilder ab4 =
        AnnotationFactory.saf.beginAnnotation(
            "foo.ArrayAnno", Annotations.asRetentionClass, "testSceneLib");
    ArrayBuilder ab4ab = ab4.beginArrayField("array", new ArrayAFT(BasicAFT.forType(double.class)));
    ab4ab.appendElement(5.0);
    ab4ab.finish();
    Annotation a4 = ab4.finish();
    Annotation tla4 = a4;

    // try combining unifiable _top-level_ annotations
    AScene secondScene = newScene();
    AClass secondSceneClazz = secondScene.classes.getVivify("bar.Test");
    secondSceneClazz.tlAnnotationsHere.add(tla1);
    // Oops--the keyed set gives us an exception if we try to put two
    // different foo.ArrayAnnos on the same class!
    AClass secondSceneClazz2 = secondScene.classes.getVivify("bar.Test2");
    secondSceneClazz2.tlAnnotationsHere.add(tla4);

    // it should be legal to write this
    StringWriter secondSW = new StringWriter();
    IndexFileWriter.write(secondScene, secondSW);

    // add an incompatible annotation
    AClass secondSceneClazz3 = secondScene.classes.getVivify("bar.Test3");
    secondSceneClazz3.tlAnnotationsHere.add(tla2);

    // now we should get a DefException
    StringWriter secondSceneSW2 = new StringWriter();
    try {
      IndexFileWriter.write(secondScene, secondSceneSW2);
      // we should have gotten an exception
      Assert.fail();
    } catch (DefException de) {
      Assert.assertTrue(
          de.getMessage().startsWith("Conflicting definitions of annotation type foo.ArrayAnno"));
      // success
    }
  }

  @Test
  public void testEmptyArrayIO() throws Exception {
    // should succeed
    String index1 =
        "package: annotation @Foo: @Retention(CLASS)\n  unknown[] arr\n"
            + "class Bar: @Foo(arr={})";
    AScene scene1 = newScene();
    IndexFileParser.parseString(index1, "testEmptyArrayIO()", scene1);

    // should reject nonempty array
    String index2 =
        "package: annotation @Foo:  @Retention(CLASS)\n unknown[] arr\n"
            + "class Bar: @Foo(arr={1})";
    AScene scene2 = newScene();
    try {
      IndexFileParser.parseString(index2, "testEmptyArrayIO()", scene2);
      // should have gotten an exception
      Assert.fail();
    } catch (FileIOException e) {
      // success
    }

    // construct a scene programmatically
    AScene scene3 = newScene();
    AClass clazz3 = scene3.classes.getVivify("Bar");
    AnnotationBuilder ab =
        AnnotationFactory.saf.beginAnnotation("Foo", Annotations.asRetentionClass, "testSceneLib");
    ab.addEmptyArrayField("arr");
    Annotation a = ab.finish();
    Annotation tla = a;
    clazz3.tlAnnotationsHere.add(tla);

    Assert.assertEquals(scene1, scene3);

    // when we write the scene out, the index file should contain the
    // special unknown[] field type
    StringWriter sw3 = new StringWriter();
    IndexFileWriter.write(scene3, sw3);
    String index3 = sw3.toString();
    Assert.assertTrue(index3.indexOf("unknown[]") >= 0);

    // can we read it back in and get the same thing?
    AScene scene4 = newScene();
    IndexFileParser.parseString(index3, "testEmptyArrayIO()", scene4);
    Assert.assertEquals(scene3, scene4);
  }

  @Test
  public void testPrune() {
    AScene s1 = newScene(), s2 = newScene();
    Assert.assertTrue(s1.equals(s2));

    s1.classes.getVivify("Foo");
    Assert.assertFalse(s1.equals(s2));

    s1.prune();
    Assert.assertTrue(s1.isEmpty());
    Assert.assertTrue(s1.equals(s2));

    Annotation sa =
        AnnotationFactory.saf
            .beginAnnotation("Anno", Annotations.asRetentionClass, "testSceneLib")
            .finish();
    Annotation tla = sa;

    AClass clazz2 = s2.classes.getVivify("Bar");
    clazz2.tlAnnotationsHere.add(tla);

    Assert.assertFalse(s1.equals(s2));
    s2.prune();
    Assert.assertFalse(s2.isEmpty());
    Assert.assertFalse(s1.equals(s2));
  }

  static class MyTAST {
    final int id;

    MyTAST(int id) {
      this.id = id;
    }

    MyTAST element = null;
    MyTAST[] typeArgs = null;

    static MyTAST arrayOf(int id, MyTAST element) {
      MyTAST t = new MyTAST(id);
      t.element = element;
      return t;
    }

    static MyTAST parameterization(int id, MyTAST... args) {
      MyTAST t = new MyTAST(id);
      t.typeArgs = args;
      return t;
    }
  }

  /*
  private static final AnnotationDef idAnnoDef =
      new AnnotationDef("IdAnno", null, Collections.singletonMap(
              "id", BasicAFT.forType(int.class)));
  */
  private static final AnnotationDef idAnnoTLDef =
      new AnnotationDef(
          "IdAnno",
          Annotations.asRetentionClass,
          Collections.singletonMap("id", BasicAFT.forType(int.class)),
          "TestSceneLib.java");

  static Annotation makeTLIdAnno(int id) {
    return new Annotation(idAnnoTLDef, Collections.singletonMap("id", Integer.valueOf(id)));
  }

  static class MyTASTMapper extends TypeASTMapper<MyTAST> {
    boolean[] saw = new boolean[11];

    @Override
    protected MyTAST getElementType(MyTAST n) {
      return n.element;
    }

    @Override
    protected MyTAST getTypeArgument(MyTAST n, int index) {
      return n.typeArgs[index];
    }

    @Override
    protected int numTypeArguments(MyTAST n) {
      return n.typeArgs == null ? 0 : n.typeArgs.length;
    }

    @Override
    protected void map(MyTAST n, ATypeElement e) {
      int nodeID = n.id;
      if (nodeID == 10) {
        Assert.assertTrue(e.lookup("IdAnno") == null);
        e.tlAnnotationsHere.add(makeTLIdAnno(10));
      } else {
        int annoID = (Integer) e.lookup("IdAnno").getFieldValue("id");
        Assert.assertEquals(nodeID, annoID);
      }
      Assert.assertFalse(saw[nodeID]);
      saw[nodeID] = true;
    }
  }

  private void assignId(ATypeElement myField, int id, Integer... ls) {
    AElement el =
        myField.innerTypes.getVivify(
            TypePathEntry.getTypePathEntryListFromBinary(Arrays.asList(ls)));
    el.tlAnnotationsHere.add(makeTLIdAnno(id));
  }

  @Test
  public void testTypeASTMapper() {
    // Construct a TAST for the type structure:
    // 0< 3<4>[1][2], 5<6, 8[7], 9, 10> >
    MyTAST tast =
        MyTAST.parameterization(
            0,
            MyTAST.arrayOf(1, MyTAST.arrayOf(2, MyTAST.parameterization(3, new MyTAST(4)))),
            MyTAST.parameterization(
                5, new MyTAST(6), MyTAST.arrayOf(7, new MyTAST(8)), new MyTAST(9), new MyTAST(10)));

    // Pretend myField represents a field of the type represented by tast.
    // We have to do this because clients are no longer allowed to create
    // AElements directly; instead, they must vivify.
    AElement myAField = new AScene().classes.getVivify("someclass").fields.getVivify("somefield");
    ATypeElement myAFieldType = myAField.type;
    // load it with annotations we can check against IDs
    myAFieldType.tlAnnotationsHere.add(makeTLIdAnno(0));

    final int ARRAY = TypePath.ARRAY_ELEMENT;
    final int TYPE_ARGUMENT = TypePath.TYPE_ARGUMENT;

    assignId(myAFieldType, 1, TYPE_ARGUMENT, 0);
    assignId(myAFieldType, 2, TYPE_ARGUMENT, 0, ARRAY, 0);
    assignId(myAFieldType, 3, TYPE_ARGUMENT, 0, ARRAY, 0, ARRAY, 0);
    assignId(myAFieldType, 4, TYPE_ARGUMENT, 0, ARRAY, 0, ARRAY, 0, TYPE_ARGUMENT, 0);
    assignId(myAFieldType, 5, TYPE_ARGUMENT, 1);
    assignId(myAFieldType, 6, TYPE_ARGUMENT, 1, TYPE_ARGUMENT, 0);
    assignId(myAFieldType, 7, TYPE_ARGUMENT, 1, TYPE_ARGUMENT, 1);
    assignId(myAFieldType, 8, TYPE_ARGUMENT, 1, TYPE_ARGUMENT, 1, ARRAY, 0);
    assignId(myAFieldType, 9, TYPE_ARGUMENT, 1, TYPE_ARGUMENT, 2);
    // to test vivification, we don't assign 10

    // now visit and make sure the ID numbers match up
    MyTASTMapper mapper = new MyTASTMapper();
    mapper.traverse(tast, myAFieldType);

    for (int i = 0; i < 11; i++) {
      Assert.assertTrue(mapper.saw[i]);
    }
    // make sure it vivified #10 and our annotation stuck
    AElement e10 =
        myAFieldType.innerTypes.get(
            TypePathEntry.getTypePathEntryListFromBinary(
                Arrays.asList(TYPE_ARGUMENT, 1, TYPE_ARGUMENT, 3)));
    Assert.assertNotNull(e10);
    int e10aid = (Integer) e10.lookup("IdAnno").getFieldValue("id");
    Assert.assertEquals(e10aid, 10);
  }
}
