/*
 * @test
 * @summary Test for crash
 *
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker MyDialog.java -nowarn
 */
public class MyDialog extends javax.swing.JDialog {}
