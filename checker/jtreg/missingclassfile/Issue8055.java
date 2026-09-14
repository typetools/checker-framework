/*
 * @test
 * @summary The Checker Framework must not crash when it completes a classpath class whose
 *   supertype mentions a class that is not on the classpath.  javac throws
 *   com.sun.tools.javac.code.Symbol$CompletionFailure and nothing catches it.  Reduced from two
 *   crashes running the Tainting Checker on Apache Beam:
 *
 *   runners/google-cloud-dataflow-java/.../util/DefaultCoderCloudObjectTranslatorRegistrar.java
 *   line 111, the class literal TableRowJsonCoder.class in the initializer of KNOWN_ATOMIC_CODERS.
 *   TableRowJsonCoder extends AtomicCoder<TableRow>, and
 *   com.google.api.services.bigquery.model.TableRow is not on the compile classpath of the
 *   runners:google-cloud-dataflow-java module.  Crashes with
 *   "error: class file for ... not found", because CFAbstractAnalysis#callTransferFunction
 *   rethrows the CompletionFailure as new BugInCF(node.getTree(), t).
 *
 *   sdks/java/extensions/sql/.../meta/provider/datastore/DataStoreV1TableProvider.java line 49,
 *   whose getTableStatistics calls
 *   DatastoreIO.v1().read().withProjectId(...).getNumEntities(...).  DatastoreV1.Read extends
 *   PTransform<PBegin, PCollection<Entity>>, and com.google.datastore.v1.Entity is not on the
 *   compile classpath of the sdks:java:extensions:sql module.  Crashes with
 *   "error: SourceChecker.typeProcess: unexpected Throwable (CompletionFailure)", because the
 *   CompletionFailure is thrown outside dataflow and reaches the catch (Throwable t) in
 *   SourceChecker#typeProcess.  The reported position is the start of the class declaration, which
 *   in Beam is the line holding the class's AutoService annotation.
 *
 *   AnnotatedTypeFactory and ElementUtils#isElementFromByteCode already catch CompletionFailure
 *   and issue a warning instead (issues 309 and 348).  These paths do not.
 *
 * @ignore Re-enable once the CompletionFailure crash is fixed (issue number 8055)
 *
 * @compile libsrc/Box.java libsrc/Missing.java libsrc/SuperTypeArg.java libsrc/Factory.java libsrc/MemberOnly.java
 * @build DeleteMissingClassFile
 * @run main DeleteMissingClassFile
 *
 * Ok.java is checked first, so that the "must stay clean" case is exercised even while the
 * crashing cases below still fail.  jtreg stops a test at its first failing action, so until the
 * crash is fixed only the first crashing shape below is reached; all five run once it is fixed.
 *
 * @compile -processor org.checkerframework.checker.tainting.TaintingChecker Ok.java
 * @compile -processor org.checkerframework.checker.tainting.TaintingChecker ClassLiteral.java
 * @compile -processor org.checkerframework.checker.tainting.TaintingChecker Field.java
 * @compile -processor org.checkerframework.checker.tainting.TaintingChecker MethodCall.java
 * @compile -processor org.checkerframework.checker.tainting.TaintingChecker Parameter.java
 */

/*
 * Each shape is in its own file because a crash aborts the whole compilation unit, so only one
 * crashing shape per file is observable.  Ok.java holds the variants that must keep compiling
 * cleanly; it pins down that the absent class has to appear in a type argument of the supertype.
 *
 * The library sources are in libsrc/ rather than lib/ on purpose.  jtreg compiles each file with
 * -sourcepath <test directory>, so if they were in lib/ javac would find lib/Missing.java there and
 * recompile the very class file that DeleteMissingClassFile just deleted, and nothing would crash.
 * With package lib declared from a directory named libsrc, sourcepath lookup cannot find them and
 * javac must read the class files that the compile step produced.
 *
 * Two further shapes are deliberately absent, because javac itself completes the symbol during
 * attribution and reports a normal "error: cannot access Missing", so the Checker Framework never
 * runs: extending SuperTypeArg, and reading a MemberOnly member whose type is the absent class.
 */
public class Issue8055 {}
