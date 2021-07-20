/*
 * @test
 * @summary Test case for Issue 1456 https://github.com/typetools/checker-framework/issues/1456
 * @compile -XDrawDiagnostics -Xlint:unchecked ../issue1456lib/Lib.java
 * @compile/fail/ref=WithStub.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.tainting.TaintingChecker -Anomsgtext Main.java -Astubs=Lib.astub -AstubWarnIfNotFound -Werror
 * @compile/fail/ref=WithoutStub.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.tainting.TaintingChecker -Anomsgtext Main.java -AstubWarnIfNotFound -Werror
 */
package issue1456;

import issue1456lib.Lib;

import org.checkerframework.checker.tainting.qual.Untainted;

public class Main {

    void test(Lib lib) {
        @Untainted Object o = lib.object;
        @Untainted byte @Untainted [] b = lib.byteArray;
        @Untainted Object o1 = lib.object1;
        @Untainted Object o2 = lib.object2;
        @Untainted byte b2 = lib.byte1;
        @Untainted byte @Untainted [] b3 = lib.byteArray2;
        byte @Untainted [] @Untainted [] b4 = lib.byteArray3;
    }

    void test2(Lib l) {
        Lib f = new Lib(l);
    }
}
