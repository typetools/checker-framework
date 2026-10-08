package org.checkerframework.framework.test.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.lang.model.type.TypeKind;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NumericalAdditionNode;
import org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests the CFG unboxing conversions for wrapper-bounded type variables. */
public class TypeVariableUnboxingCFGTest {

  /** Temporary directory for compiler inputs and outputs. */
  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  /** Creates a TypeVariableUnboxingCFGTest. */
  public TypeVariableUnboxingCFGTest() {}

  @Test
  public void unboxLeftOperand() throws IOException {
    ControlFlowGraph cfg = generateCFG("add");
    cfg.checkInvariants();
    NumericalAdditionNode addition =
        cfg.getAllNodes().stream()
            .filter(NumericalAdditionNode.class::isInstance)
            .map(NumericalAdditionNode.class::cast)
            .findFirst()
            .orElseThrow();
    assertUnboxing(addition.getLeftOperand(), "intValue", TypeKind.INT);
  }

  @Test
  public void unboxRightOperand() throws IOException {
    ControlFlowGraph cfg = generateCFG("subtract");
    cfg.checkInvariants();
    NumericalSubtractionNode subtraction =
        cfg.getAllNodes().stream()
            .filter(NumericalSubtractionNode.class::isInstance)
            .map(NumericalSubtractionNode.class::cast)
            .findFirst()
            .orElseThrow();
    assertUnboxing(subtraction.getRightOperand(), "longValue", TypeKind.LONG);
  }

  /**
   * Builds a CFG from a temporary copy of the existing numeric-promotion regression input.
   *
   * @param method the method whose CFG is built
   * @return the method's CFG
   * @throws IOException if copying the test input fails
   */
  private ControlFlowGraph generateCFG(String method) throws IOException {
    Path inputFile = temporaryFolder.getRoot().toPath().resolve("TypeVarPrimitives.java");
    Files.copy(Path.of("tests/all-systems/TypeVarPrimitives.java"), inputFile);
    return CFGVisualizeLauncher.generateMethodCFG(
        inputFile.toString(), method, "TypeVarPrimitives", null);
  }

  /**
   * Checks that an operand is unboxed by a primitive-value method call with a null-pointer edge.
   *
   * @param operand the converted operand of a numeric operation
   * @param methodName the expected primitive-value method name
   * @param primitiveKind the expected primitive result type
   */
  private void assertUnboxing(Node operand, String methodName, TypeKind primitiveKind) {
    assertTrue(
        "Expected an unboxing method invocation: " + operand,
        operand instanceof MethodInvocationNode);
    MethodAccessNode access = ((MethodInvocationNode) operand).getTarget();
    assertEquals(methodName, access.getMethod().getSimpleName().toString());
    assertEquals(TypeKind.TYPEVAR, access.getReceiver().getType().getKind());
    assertEquals(primitiveKind, operand.getType().getKind());
    Block block = access.getBlock();
    assertTrue("Expected an exception block for unboxing", block instanceof ExceptionBlock);
    assertTrue(
        ((ExceptionBlock) block)
            .getExceptionalSuccessors().entrySet().stream()
                .anyMatch(
                    entry ->
                        entry.getKey().toString().equals("java.lang.NullPointerException")
                            && !entry.getValue().isEmpty()));
  }
}
