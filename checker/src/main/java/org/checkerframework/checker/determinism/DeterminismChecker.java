package org.checkerframework.checker.determinism;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * The Determinism Checker prevents non-determinism in single-threaded programs. This type checker
 * enables a programmer to indicate which computations should be the same across runs of a program,
 * and then verifies that property. Examples of causes of non-determinism include the use of Random,
 * Date and Time classes. Another cause is use of hash tables (including HashSets and HashMaps),
 * whose iteration order is nondeterministic across runs. For example, even if the set contains the
 * same values, the following code may produce output in different orders:
 *
 * <pre>
 *     for (Object x : mySet) {
 *     System.out.println(x);
 *     }
 * </pre>
 *
 * Another common cause of nondeterminism is the operating system scheduler, for concurrent
 * programs. The Determinism Checker does not address this cause of nondeterminism.
 *
 * @checker_framework.manual #determinism-checker Determinism Checker
 */
public class DeterminismChecker extends BaseTypeChecker {}
