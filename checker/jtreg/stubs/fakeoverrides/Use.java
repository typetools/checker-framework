package fakeoverrides;

import java.util.List;
import java.util.Map;
import org.checkerframework.checker.tainting.qual.Untainted;

/*
 * @test
 * @summary Test case for multiple fake overrides applying to a callsite.
 *
 * @compile DefineClasses.java
 * @compile -processor org.checkerframework.checker.tainting.TaintingChecker -Astubs=DefineClasses.astub -AstubWarnIfNotFound -Werror Use.java
 */
// TODO: Issue error SuperClass and SubInterface have conflicting fake overrides
// See https://github.com/typetools/checker-framework/issues/2724
public class Use extends SuperClass implements SubInterface {
  void use(Use d) {
    // Ok, because the fake override in SuperClasses is taken over the one in SubInterface.
    @Untainted int i = d.m();
  }

  void useGeneric(Use d, List<String> l) {
    // Ok, because of the fake override in SuperClass.
    @Untainted int i = d.g(l);
  }

  void useNested(Use d, Map.Entry<String, String> e) {
    // Ok, because of the fake override in SuperClass.
    @Untainted int i = d.nested(e);
  }

  void useWildcard(Use d, List<? extends Number> l) {
    // Ok, because of the fake override in SuperClass.
    @Untainted int i = d.wildcard(l);
  }

  void useVarargs(Use d, String s) {
    // Ok, because of the fake override in SuperClass.
    @Untainted int i = d.varargs(s);
  }

  void useEnclosing(Use d, Enclosing<String>.Nested<Integer> e) {
    // Ok, because of the fake override in SuperClass.
    @Untainted int i = d.enclosing(e);
  }

  void useIntersection(Use d, Integer n) {
    // Ok, because of the fake override in SuperClass.
    @Untainted int i = d.intersection(n);
  }
}
