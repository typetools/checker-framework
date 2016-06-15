package org.checkerframework.stubparser;

import java.util.ArrayList;
import java.util.List;

import org.checkerframework.stubparser.ast.CompilationUnit;
import org.checkerframework.stubparser.ast.body.BodyDeclaration;
import org.checkerframework.stubparser.ast.body.FieldDeclaration;
import org.checkerframework.stubparser.ast.body.MethodDeclaration;
import org.checkerframework.stubparser.ast.body.Parameter;
import org.checkerframework.stubparser.ast.body.TypeDeclaration;
import org.checkerframework.stubparser.ast.body.VariableDeclarator;
import org.checkerframework.stubparser.ast.body.VariableDeclaratorId;
import org.checkerframework.stubparser.ast.expr.Expression;
import org.checkerframework.stubparser.ast.expr.MethodCallExpr;
import org.checkerframework.stubparser.ast.expr.NameExpr;
import org.checkerframework.stubparser.ast.expr.QualifiedNameExpr;
import org.checkerframework.stubparser.ast.expr.VariableDeclarationExpr;
import org.checkerframework.stubparser.ast.stmt.BlockStmt;
import org.checkerframework.stubparser.ast.stmt.ExpressionStmt;
import org.checkerframework.stubparser.ast.stmt.Statement;
import org.checkerframework.stubparser.ast.type.ClassOrInterfaceType;
import org.checkerframework.stubparser.ast.type.PrimitiveType;
import org.checkerframework.stubparser.ast.type.ReferenceType;
import org.checkerframework.stubparser.ast.type.Type;
import org.checkerframework.stubparser.ast.type.VoidType;
import org.checkerframework.stubparser.ast.type.PrimitiveType.Primitive;

/**
 * This class helps to construct new nodes.
 * 
 * @author Julio Vilmar Gesser
 */
public final class ASTHelper {

    public static final PrimitiveType BYTE_TYPE = new PrimitiveType(Primitive.Byte);

    public static final PrimitiveType SHORT_TYPE = new PrimitiveType(Primitive.Short);

    public static final PrimitiveType INT_TYPE = new PrimitiveType(Primitive.Int);

    public static final PrimitiveType LONG_TYPE = new PrimitiveType(Primitive.Long);

    public static final PrimitiveType FLOAT_TYPE = new PrimitiveType(Primitive.Float);

    public static final PrimitiveType DOUBLE_TYPE = new PrimitiveType(Primitive.Double);

    public static final PrimitiveType BOOLEAN_TYPE = new PrimitiveType(Primitive.Boolean);

    public static final PrimitiveType CHAR_TYPE = new PrimitiveType(Primitive.Char);

    public static final VoidType VOID_TYPE = new VoidType();

    private ASTHelper() {
        // nop
    }

    /**
     * Creates a new {@link NameExpr} from a qualified name.<br>
     * The qualified name can contains "." (dot) characters.
     * 
     * @param qualifiedName
     *            qualified name
     * @return instanceof {@link NameExpr}
     */
    public static NameExpr createNameExpr(String qualifiedName) {
        String[] split = qualifiedName.split("\\.");
        NameExpr ret = new NameExpr(split[0]);
        for (int i = 1; i < split.length; i++) {
            ret = new QualifiedNameExpr(ret, split[i]);
        }
        return ret;
    }

    /**
     * Creates a new {@link Parameter}.
     * 
     * @param type
     *            type of the parameter
     * @param name
     *            name of the parameter
     * @return instance of {@link Parameter}
     */
    public static Parameter createParameter(Type type, String name) {
        return new Parameter(type, new VariableDeclaratorId(name));
    }

    /**
     * Creates a {@link FieldDeclaration}.
     * 
     * @param modifiers
     *            modifiers
     * @param type
     *            type
     * @param variable
     *            variable declarator
     * @return instance of {@link FieldDeclaration}
     */
    public static FieldDeclaration createFieldDeclaration(int modifiers, Type type, VariableDeclarator variable) {
        List<VariableDeclarator> variables = new ArrayList<VariableDeclarator>();
        variables.add(variable);
        FieldDeclaration ret = new FieldDeclaration(modifiers, type, variables);
        return ret;
    }

    /**
     * Creates a {@link FieldDeclaration}.
     * 
     * @param modifiers
     *            modifiers
     * @param type
     *            type
     * @param name
     *            field name
     * @return instance of {@link FieldDeclaration}
     */
    public static FieldDeclaration createFieldDeclaration(int modifiers, Type type, String name) {
        VariableDeclaratorId id = new VariableDeclaratorId(name);
        VariableDeclarator variable = new VariableDeclarator(id);
        return createFieldDeclaration(modifiers, type, variable);
    }

    /**
     * Creates a {@link VariableDeclarationExpr}.
     * 
     * @param type
     *            type
     * @param name
     *            name
     * @return instance of {@link VariableDeclarationExpr}
     */
    public static VariableDeclarationExpr createVariableDeclarationExpr(Type type, String name) {
        List<VariableDeclarator> vars = new ArrayList<VariableDeclarator>();
        vars.add(new VariableDeclarator(new VariableDeclaratorId(name)));
        return new VariableDeclarationExpr(type, vars);
    }

    /**
     * Adds the given parameter to the method. The list of parameters will be
     * initialized if it is {@code null}.
     * 
     * @param method
     *            method
     * @param parameter
     *            parameter
     */
    public static void addParameter(MethodDeclaration method, Parameter parameter) {
        List<Parameter> parameters = method.getParameters();
        if (parameters == null) {
            parameters = new ArrayList<Parameter>();
            method.setParameters(parameters);
        }
        parameters.add(parameter);
    }

    /**
     * Adds the given argument to the method call. The list of arguments will be
     * initialized if it is {@code null}.
     * 
     * @param call
     *            method call
     * @param arg
     *            argument value
     */
    public static void addArgument(MethodCallExpr call, Expression arg) {
        List<Expression> args = call.getArgs();
        if (args == null) {
            args = new ArrayList<Expression>();
            call.setArgs(args);
        }
        args.add(arg);
    }

    /**
     * Adds the given type declaration to the compilation unit. The list of
     * types will be initialized if it is {@code null}.
     * 
     * @param cu
     *            compilation unit
     * @param type
     *            type declaration
     */
    public static void addTypeDeclaration(CompilationUnit cu, TypeDeclaration type) {
        List<TypeDeclaration> types = cu.getTypes();
        if (types == null) {
            types = new ArrayList<TypeDeclaration>();
            cu.setTypes(types);
        }
        types.add(type);

    }

    /**
     * Creates a new {@link ReferenceType} for a class or interface.
     * 
     * @param name
     *            name of the class or interface
     * @param arrayCount
     *            number os arrays or {@code 0} if is not a array.
     * @return instanceof {@link ReferenceType}
     */
    public static ReferenceType createReferenceType(String name, int arrayCount) {
        return new ReferenceType(new ClassOrInterfaceType(name), arrayCount);
    }

    /**
     * Creates a new {@link ReferenceType} for the given primitive type.
     * 
     * @param type
     *            primitive type
     * @param arrayCount
     *            number os arrays or {@code 0} if is not a array.
     * @return instanceof {@link ReferenceType}
     */
    public static ReferenceType createReferenceType(PrimitiveType type, int arrayCount) {
        return new ReferenceType(type, arrayCount);
    }

    /**
     * Adds the given statement to the specified block. The list of statements
     * will be initialized if it is {@code null}.
     */
    public static void addStmt(BlockStmt block, Statement stmt) {
        List<Statement> stmts = block.getStmts();
        if (stmts == null) {
            stmts = new ArrayList<Statement>();
            block.setStmts(stmts);
        }
        stmts.add(stmt);
    }

    /**
     * Adds the given expression to the specified block. The list of statements
     * will be initialized if it is {@code null}.
     */
    public static void addStmt(BlockStmt block, Expression expr) {
        addStmt(block, new ExpressionStmt(expr));
    }

    /**
     * Adds the given declaration to the specified type. The list of members
     * will be initialized if it is {@code null}.
     * 
     * @param type
     *            type declaration
     * @param decl
     *            member declaration
     */
    public static void addMember(TypeDeclaration type, BodyDeclaration decl) {
        List<BodyDeclaration> members = type.getMembers();
        if (members == null) {
            members = new ArrayList<BodyDeclaration>();
            type.setMembers(members);
        }
        members.add(decl);
    }

}
