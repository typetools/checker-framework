package fakeoverrides;

import org.checkerframework.checker.tainting.qual.Untainted;

/*
 * @test
 * @summary Test case for multiple fake overrides applying to a callsite.
 *
 * @compile -XDrawDiagnostics DefineClasses.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -Astubs=DefineClasses.astub -AstubWarnIfNotFound -Werror Use.java
 */
// TODO: Issue error SuperClass and SubInterface have conflicting fake overrides
// See https://github.com/typetools/checker-framework/issues/2724
public class Use extends SuperClass implements SubInterface {
  void use(Use d) {
    // Ok, because the fake override in SuperClasses is taken over the one in SubInterface.
    @Untainted int i = d.m();
  }
}
