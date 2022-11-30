/*
 * @test
 * @summary Test for crash
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker MyDialog.java -nowarn
 */
public class MyDialog extends javax.swing.JDialog {}
