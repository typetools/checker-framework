package com.sun.source.tree;

import checkers.javari.quals.*;

public interface Tree {

    @ReadOnly public enum Kind {
        ANNOTATION(AnnotationTree.class),
        ARRAY_ACCESS(ArrayAccessTree.class),
        ARRAY_TYPE(ArrayTypeTree.class),
        ASSERT(AssertTree.class),
        ASSIGNMENT(AssignmentTree.class),
        BLOCK(BlockTree.class),
        BREAK(BreakTree.class),
        CASE(CaseTree.class),
        CATCH(CatchTree.class),
        CLASS(ClassTree.class),
        COMPILATION_UNIT(CompilationUnitTree.class),
        CONDITIONAL_EXPRESSION(ConditionalExpressionTree.class),
        CONTINUE(ContinueTree.class),
        DO_WHILE_LOOP(DoWhileLoopTree.class),
        ENHANCED_FOR_LOOP(EnhancedForLoopTree.class),
        EXPRESSION_STATEMENT(ExpressionStatementTree.class),
        MEMBER_SELECT(MemberSelectTree.class),
        FOR_LOOP(ForLoopTree.class),
        IDENTIFIER(IdentifierTree.class),
        IF(IfTree.class),
        IMPORT(ImportTree.class),
        INSTANCE_OF(InstanceOfTree.class),
        LABELED_STATEMENT(LabeledStatementTree.class),
        METHOD(MethodTree.class),
        METHOD_INVOCATION(MethodInvocationTree.class),
        MODIFIERS(ModifiersTree.class),
        NEW_ARRAY(NewArrayTree.class),
        NEW_CLASS(NewClassTree.class),
        PARENTHESIZED(ParenthesizedTree.class),
        PRIMITIVE_TYPE(PrimitiveTypeTree.class),
        RETURN(ReturnTree.class),
        EMPTY_STATEMENT(EmptyStatementTree.class),
        SWITCH(SwitchTree.class),
        SYNCHRONIZED(SynchronizedTree.class),
        THROW(ThrowTree.class),
        TRY(TryTree.class),
        PARAMETERIZED_TYPE(ParameterizedTypeTree.class),
        TYPE_CAST(TypeCastTree.class),
        TYPE_PARAMETER(TypeParameterTree.class),
        VARIABLE(VariableTree.class),
        WHILE_LOOP(WhileLoopTree.class),
        POSTFIX_INCREMENT(UnaryTree.class),
        POSTFIX_DECREMENT(UnaryTree.class),
        PREFIX_INCREMENT(UnaryTree.class),
        PREFIX_DECREMENT(UnaryTree.class),
        UNARY_PLUS(UnaryTree.class),
        UNARY_MINUS(UnaryTree.class),
        BITWISE_COMPLEMENT(UnaryTree.class),
        LOGICAL_COMPLEMENT(UnaryTree.class),
        MULTIPLY(BinaryTree.class),
        DIVIDE(BinaryTree.class),
        REMAINDER(BinaryTree.class),
        PLUS(BinaryTree.class),
        MINUS(BinaryTree.class),
        LEFT_SHIFT(BinaryTree.class),
        RIGHT_SHIFT(BinaryTree.class),
        UNSIGNED_RIGHT_SHIFT(BinaryTree.class),
        LESS_THAN(BinaryTree.class),
        GREATER_THAN(BinaryTree.class),
        LESS_THAN_EQUAL(BinaryTree.class),
        GREATER_THAN_EQUAL(BinaryTree.class),
        EQUAL_TO(BinaryTree.class),
        NOT_EQUAL_TO(BinaryTree.class),
        AND(BinaryTree.class),
        XOR(BinaryTree.class),
        OR(BinaryTree.class),
        CONDITIONAL_AND(BinaryTree.class),
        CONDITIONAL_OR(BinaryTree.class),
        MULTIPLY_ASSIGNMENT(CompoundAssignmentTree.class),
        DIVIDE_ASSIGNMENT(CompoundAssignmentTree.class),
        REMAINDER_ASSIGNMENT(CompoundAssignmentTree.class),
        PLUS_ASSIGNMENT(CompoundAssignmentTree.class),
        MINUS_ASSIGNMENT(CompoundAssignmentTree.class),
        LEFT_SHIFT_ASSIGNMENT(CompoundAssignmentTree.class),
        RIGHT_SHIFT_ASSIGNMENT(CompoundAssignmentTree.class),
        UNSIGNED_RIGHT_SHIFT_ASSIGNMENT(CompoundAssignmentTree.class),
        AND_ASSIGNMENT(CompoundAssignmentTree.class),
        ANNOTATED_TYPE(AnnotatedTypeTree.class),
        XOR_ASSIGNMENT(CompoundAssignmentTree.class),
        OR_ASSIGNMENT(CompoundAssignmentTree.class),
        INT_LITERAL(LiteralTree.class),
        LONG_LITERAL(LiteralTree.class),
        FLOAT_LITERAL(LiteralTree.class),
        DOUBLE_LITERAL(LiteralTree.class),
        BOOLEAN_LITERAL(LiteralTree.class),
        CHAR_LITERAL(LiteralTree.class),
        STRING_LITERAL(LiteralTree.class),
        NULL_LITERAL(LiteralTree.class),
        UNBOUNDED_WILDCARD(WildcardTree.class),
        EXTENDS_WILDCARD(WildcardTree.class),
        SUPER_WILDCARD(WildcardTree.class),
        ERRONEOUS(ErroneousTree.class),
        OTHER(null);


        Kind(Class<? extends Tree> intf) {
            throw new RuntimeException("skeleton method");
        }

        public Class<? extends Tree> asInterface() {
            throw new RuntimeException("skeleton method");
        }

        private final Class<? extends Tree> associatedInterface;
    }

    @PolyRead Kind getKind(@PolyRead Tree this);
    <R,D> R accept(@ReadOnly Tree this, TreeVisitor<R,D> visitor, D data);
}
