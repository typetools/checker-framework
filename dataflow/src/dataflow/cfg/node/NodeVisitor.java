package dataflow.cfg.node;

/**
 * A visitor for a {@link Node} tree.
 *
 * @author Stefan Heule
 *
 * @param <R>
 *            Return type of the visitor. Use {@link Void} if the visitor does
 *            not have a return value.
 * @param <P>
 *            Parameter type of the visitor. Use {@link Void} if the visitor
 *            does not have a parameter.
 */
public interface NodeVisitor<R, P> {
    // Literals
    R visitShortLiteral(ShortLiteralNode n, P p);

    R visitIntegerLiteral(IntegerLiteralNode n, P p);

    R visitLongLiteral(LongLiteralNode n, P p);

    R visitFloatLiteral(FloatLiteralNode n, P p);

    R visitDoubleLiteral(DoubleLiteralNode n, P p);

    R visitBooleanLiteral(BooleanLiteralNode n, P p);

    R visitCharacterLiteral(CharacterLiteralNode n, P p);

    R visitStringLiteral(StringLiteralNode n, P p);

    R visitNullLiteral(NullLiteralNode n, P p);

    // Unary operations
    R visitNumericalMinus(NumericalMinusNode n, P p);

    R visitNumericalPlus(NumericalPlusNode n, P p);

    R visitBitwiseComplement(BitwiseComplementNode n, P p);

    // Binary operations
    R visitStringConcatenate(StringConcatenateNode n, P p);

    R visitNumericalAddition(NumericalAdditionNode n, P p);

    R visitNumericalSubtraction(NumericalSubtractionNode n, P p);

    R visitNumericalMultiplication(NumericalMultiplicationNode n, P p);

    R visitIntegerDivision(IntegerDivisionNode n, P p);

    R visitFloatingDivision(FloatingDivisionNode n, P p);

    R visitIntegerRemainder(IntegerRemainderNode n, P p);

    R visitFloatingRemainder(FloatingRemainderNode n, P p);

    R visitLeftShift(LeftShiftNode n, P p);

    R visitSignedRightShift(SignedRightShiftNode n, P p);

    R visitUnsignedRightShift(UnsignedRightShiftNode n, P p);

    R visitBitwiseAnd(BitwiseAndNode n, P p);

    R visitBitwiseOr(BitwiseOrNode n, P p);

    R visitBitwiseXor(BitwiseXorNode n, P p);

    // Increment and decrement operations
    R visitPrefixIncrement(PrefixIncrementNode n, P p);

    R visitPrefixDecrement(PrefixDecrementNode n, P p);

    R visitPostfixIncrement(PostfixIncrementNode n, P p);

    R visitPostfixDecrement(PostfixDecrementNode n, P p);

    // Compound assignments
    R visitCompoundAssignment(CompoundAssignmentNode n, P p);

    R visitStringConcatenateAssignment(StringConcatenateAssignmentNode n, P p);

    R visitNumericalAdditionAssignment(NumericalAdditionAssignmentNode n, P p);

    R visitNumericalSubtractionAssignment(NumericalSubtractionAssignmentNode n,
            P p);

    R visitNumericalMultiplicationAssignment(
            NumericalMultiplicationAssignmentNode n, P p);

    R visitIntegerDivisionAssignment(IntegerDivisionAssignmentNode n, P p);

    R visitFloatingDivisionAssignment(FloatingDivisionAssignmentNode n, P p);

    R visitIntegerRemainderAssignment(IntegerRemainderAssignmentNode n, P p);

    R visitFloatingRemainderAssignment(FloatingRemainderAssignmentNode n, P p);

    R visitLeftShiftAssignment(LeftShiftAssignmentNode n, P p);

    R visitSignedRightShiftAssignment(SignedRightShiftAssignmentNode n, P p);

    R visitUnsignedRightShiftAssignment(UnsignedRightShiftAssignmentNode n, P p);

    R visitBitwiseAndAssignment(BitwiseAndAssignmentNode n, P p);

    R visitBitwiseOrAssignment(BitwiseOrAssignmentNode n, P p);

    R visitBitwiseXorAssignment(BitwiseXorAssignmentNode n, P p);

    // Comparison operations
    R visitLessThan(LessThanNode n, P p);

    R visitLessThanOrEqual(LessThanOrEqualNode n, P p);

    R visitGreaterThan(GreaterThanNode n, P p);

    R visitGreaterThanOrEqual(GreaterThanOrEqualNode n, P p);

    R visitEqualTo(EqualToNode n, P p);

    R visitNotEqual(NotEqualNode n, P p);

    // Conditional operations
    R visitConditionalAnd(ConditionalAndNode n, P p);

    R visitConditionalOr(ConditionalOrNode n, P p);

    R visitConditionalNot(ConditionalNotNode n, P p);

    R visitTernaryExpression(TernaryExpressionNode n, P p);

    R visitAssignment(AssignmentNode n, P p);

    R visitLocalVariable(LocalVariableNode n, P p);

    R visitVariableDeclaration(VariableDeclarationNode n, P p);

    R visitFieldAccess(FieldAccessNode n, P p);

    R visitMethodAccess(MethodAccessNode n, P p);

    R visitArrayAccess(ArrayAccessNode n, P p);

    R visitImplicitThisLiteral(ImplicitThisLiteralNode n, P p);

    R visitExplicitThisLiteral(ExplicitThisLiteralNode n, P p);

    R visitSuper(SuperNode n, P p);

    R visitReturn(ReturnNode n, P p);

    R visitStringConversion(StringConversionNode n, P p);

    R visitBoxing(BoxingNode n, P p);

    R visitUnboxing(UnboxingNode n, P p);

    R visitNarrowingConversion(NarrowingConversionNode n, P p);

    R visitWideningConversion(WideningConversionNode n, P p);

    R visitInstanceOf(InstanceOfNode n, P p);

    R visitTypeCast(TypeCastNode n, P p);

    // Statements
    R visitAssertionError(AssertionErrorNode n, P p);

    R visitThrow(ThrowNode n, P p);

    // Cases
    R visitCase(CaseNode n, P p);

    // Method and constructor invocations
    R visitMethodInvocation(MethodInvocationNode n, P p);

    R visitObjectCreation(ObjectCreationNode n, P p);

    R visitArrayCreation(ArrayCreationNode n, P p);

    // Type, package and class names
    R visitArrayType(ArrayTypeNode n, P p);

    R visitPrimitiveType(PrimitiveTypeNode n, P p);

    R visitClassName(ClassNameNode n, P p);

    R visitPackageName(PackageNameNode n, P p);

    // Parameterized types
    R visitParameterizedType(ParameterizedTypeNode n, P p);

    // Marker nodes
    R visitMarker(MarkerNode n, P p);
}
