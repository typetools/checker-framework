/*
 * @test
 * @summary The Checker Framework must not crash when it completes a classpath class whose
 *   supertype mentions a class that is not on the classpath.  javac completes such a symbol
 *   lazily and never needs it, so javac reports no error and the Checker Framework must not
 *   either.  Every compilation below is checked against a .goal file, so the test fails if the
 *   diagnostics change, not only if a compilation crashes.
 *
 *   Issue 8055 was two bugs, one behind the other.  First,
 *   AnnotatedTypeFactory#getDeclAnnotations called Elements#getAllAnnotationMirrors outside the
 *   try block meant to guard it -- getAllAnnotationMirrors walks the superclass chain looking
 *   for inherited annotations, and asks each superclass isErroneous(), which completes its type
 *   arguments -- so the CompletionFailure escaped.  Second, javac's failed completion leaves the
 *   absent class's symbol with kind ERR, so every ClassType naming it reports TypeKind.ERROR;
 *   once the CompletionFailure was caught, AnnotatedTypeMirror#createType rejected that ERROR
 *   type with "input is not compilable".
 *
 *   Reduced from two crashes running the Tainting Checker on Apache Beam:
 *
 *   runners/google-cloud-dataflow-java/.../util/DefaultCoderCloudObjectTranslatorRegistrar.java
 *   line 111, the class literal TableRowJsonCoder.class in the initializer of KNOWN_ATOMIC_CODERS.
 *   TableRowJsonCoder extends AtomicCoder<TableRow>, and
 *   com.google.api.services.bigquery.model.TableRow is not on the compile classpath of the
 *   runners:google-cloud-dataflow-java module.  It crashed with
 *   "error: class file for ... not found", because CFAbstractAnalysis#callTransferFunction
 *   rethrew the CompletionFailure as new BugInCF(node.getTree(), t).
 *
 *   sdks/java/extensions/sql/.../meta/provider/datastore/DataStoreV1TableProvider.java line 49,
 *   whose getTableStatistics calls
 *   DatastoreIO.v1().read().withProjectId(...).getNumEntities(...).  DatastoreV1.Read extends
 *   PTransform<PBegin, PCollection<Entity>>, and com.google.datastore.v1.Entity is not on the
 *   compile classpath of the sdks:java:extensions:sql module.  It crashed with
 *   "error: SourceChecker.typeProcess: unexpected Throwable (CompletionFailure)", because the
 *   CompletionFailure was thrown outside dataflow and reached the catch (Throwable t) in
 *   SourceChecker#typeProcess.  The reported position was the start of the class declaration,
 *   which in Beam is the line holding the class's AutoService annotation.
 *
 *   AnnotatedTypeFactory and ElementUtils#getSuperClass already caught CompletionFailure and
 *   carried on instead (issues 309 and 348).  These paths did not.
 *
 * @compile libsrc/Box.java libsrc/Missing.java libsrc/SuperTypeArg.java libsrc/Factory.java libsrc/MemberOnly.java libsrc/QualParam.java libsrc/SubOfQualParam.java
 * @build DeleteMissingClassFile
 * @run main DeleteMissingClassFile
 * @compile/ref=Ok.goal -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker Ok.java
 * @compile/ref=ClassLiteral.goal -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker ClassLiteral.java
 * @compile/ref=Field.goal -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker Field.java
 * @compile/ref=MethodCall.goal -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker MethodCall.java
 * @compile/ref=Parameter.goal -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker Parameter.java
 * @compile/fail/ref=InheritedAnno.goal -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker InheritedAnno.java
 */

/*
 * Each shape is in its own file because a crash aborts the whole compilation unit, so only one
 * crashing shape per file is observable.  Ok.java holds the variants that must keep compiling
 * cleanly; it pins down that the absent class has to appear in a type argument of the supertype.
 * It is checked first, so that the "must stay clean" case is exercised before the shapes that used
 * to crash; jtreg stops a test at its first failing action.
 *
 * Every @compile names a .goal file, because a bare @compile checks only the exit status: it would
 * pass whether the warning is issued once, five times, or not at all.  -XDrawDiagnostics is what
 * makes the golden files portable; without it a diagnostic that has a source position prints the
 * absolute path that jtreg passed to javac.
 *
 * InheritedAnno.java is the only compilation that is expected to fail, and its error is the
 * assertion: lib.SubOfQualParam inherits @HasQualifierParameter from lib.QualParam, whose own
 * supertype is the one that cannot be read, and that annotation is what makes the assignment an
 * error.  Without it the file compiles cleanly, so the error is evidence that a declaration
 * annotation is still inherited across the class whose supertype is missing.
 *
 * Keep prose out of the tag block above: jtreg treats everything after `@run main
 * DeleteMissingClassFile`, up to the next tag, as command-line arguments to its main method.
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
