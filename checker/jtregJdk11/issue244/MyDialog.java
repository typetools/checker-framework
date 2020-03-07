/*
 * @test
 * @summary Test for crash
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker MyDialog.java -target 1.8 -source 1.8 -nowarn
 */
public class MyDialog extends javax.swing.JDialog {}
