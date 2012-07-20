package checkers.util.report;

import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import checkers.basetype.BaseTypeVisitor;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.ElementUtils;
import checkers.util.TreeUtils;
import checkers.util.report.quals.*;

import com.sun.source.tree.*;

public class ReportVisitor extends BaseTypeVisitor<ReportChecker> {

    /**
     * The tree kinds that should be reported.
     */
    private final String[] treeKinds;

    /**
     * The modifiers that should be reported.
     */
    private final String[] modifiers;

    public ReportVisitor(ReportChecker checker, CompilationUnitTree root) {
        super(checker, root);

        if (options.containsKey("reportTreeKinds")) {
            String trees = options.get("reportTreeKinds");
            treeKinds = trees.split(",");
        } else {
            treeKinds = null;
        }

        if (options.containsKey("reportModifiers")) {
            String mods = options.get("reportModifiers");
            modifiers = mods.split(",");
        } else {
            modifiers = null;
        }
    }

    @Override
    public Void scan(Tree tree, Void p) {
        if (tree!=null && treeKinds!=null) {
            for (String tk : treeKinds) {
                if (tree.getKind().toString().equals(tk)) {
                    checker.report(Result.failure("Tree.Kind." + tk), tree);
                }
            }
        }
        return super.scan(tree, p);
    }

    @Override
    public Void visitClass(ClassTree node, Void p) {
        TypeElement member = TreeUtils.elementFromDeclaration(node);
        boolean report = false;
        // No need to check on the declaring class itself
        // this.atypeFactory.getDeclAnnotation(member, ReportInherit.class) != null;

        // Check whether any superclass/interface had the ReportInherit annotation.
        Set<TypeElement> suptypes = ElementUtils.getSuperTypes(member);
        for (TypeElement sup : suptypes) {
            report = this.atypeFactory.getDeclAnnotation(sup, ReportInherit.class) != null;
            if (report) {
                // Set member to report the right member, if found.
                member = sup;
                break;
            }
        }

        if (report) {
            checker.report(Result.failure("inherit", node, member), node);
        }
        return super.visitClass(node, p);
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        ExecutableElement method = TreeUtils.elementFromDeclaration(node);
        boolean report = false;

        // Check all overridden methods.
        Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
            annoTypes.overriddenMethods(method);
        for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair: overriddenMethods.entrySet()) {
            // AnnotatedDeclaredType overriddenType = pair.getKey();
            ExecutableElement exe = pair.getValue();
            report = this.atypeFactory.getDeclAnnotation(exe, ReportOverride.class) != null;
            if (report) {
                // Set method to report the right method, if found.
                method = exe;
                break;
            }
        }

        if (report) {
            checker.report(Result.failure("override", node,
                    method.getEnclosingElement().toString() + "." + method), node);
        }
        return super.visitMethod(node, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        ExecutableElement method = TreeUtils.elementFromUse(node);
        boolean report = this.atypeFactory.getDeclAnnotation(method, ReportCall.class) != null;

        if (!report) {
            // Find all methods that are overridden by the called method
            Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
                    annoTypes.overriddenMethods(method);
            for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair: overriddenMethods.entrySet()) {
                // AnnotatedDeclaredType overriddenType = pair.getKey();
                ExecutableElement exe = pair.getValue();
                report = this.atypeFactory.getDeclAnnotation(exe, ReportCall.class) != null;
                if (report) {
                    // Always report the element that has the annotation.
                    // Alternative would be to always report the initial element.
                    method = exe;
                    break;
                }
            }
        }

        if (report) {
            checker.report(Result.failure("methodcall", node,
                    method.getEnclosingElement().toString() + "." + method), node);
        }
        return super.visitMethodInvocation(node, p);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        Element member = TreeUtils.elementFromUse(node);
        boolean report = this.atypeFactory.getDeclAnnotation(member, ReportReadWrite.class) != null;

        if (report) {
            checker.report(Result.failure("fieldreadwrite", node,
                    member.getEnclosingElement().toString() + "." + member.getSimpleName()), node);
        }
        return super.visitMemberSelect(node, p);
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        Element member = TreeUtils.elementFromUse(node);
        boolean report = this.atypeFactory.getDeclAnnotation(member, ReportReadWrite.class) != null;

        if (report) {
            checker.report(Result.failure("fieldreadwrite", node,
                    member.getEnclosingElement().toString() + "." + member.getSimpleName()), node);
        }
        return super.visitIdentifier(node, p);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        Element member = TreeUtils.elementFromUse(node.getVariable());
        boolean report = this.atypeFactory.getDeclAnnotation(member, ReportWrite.class) != null;

        if (report) {
            checker.report(Result.failure("fieldwrite", node,
                    member.getEnclosingElement().toString() + "." + member.getSimpleName()), node);
        }
        return super.visitAssignment(node, p);
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
        // TODO: should we introduce an annotation for this?
        return super.visitArrayAccess(node, p);
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
        Element member = TreeUtils.elementFromUse(node);
        boolean report = this.atypeFactory.getDeclAnnotation(member, ReportCreation.class) != null;
        if (!report) {
            // If the constructor is not annotated, check whether the class is.
            member = member.getEnclosingElement();
            report = this.atypeFactory.getDeclAnnotation(member, ReportCreation.class) != null;
        }
        if (!report) {
            // Check whether any superclass/interface had the ReportCreation annotation.
            Set<TypeElement> suptypes = ElementUtils.getSuperTypes((TypeElement)member);
            for (TypeElement sup : suptypes) {
                report = this.atypeFactory.getDeclAnnotation(sup, ReportCreation.class) != null;
                if (report) {
                    // Set member to report the right member if found
                    member = sup;
                    break;
                }
            }
        }

        if (report) {
            checker.report(Result.failure("creation", node, member), node);
        }
        return super.visitNewClass(node, p);
    }

    @Override
    public Void visitNewArray(NewArrayTree node, Void p) {
        // TODO Should we report this if the array type is @ReportCreation?
        return super.visitNewArray(node, p);
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        // TODO Is it worth adding a separate annotation for this?
        return super.visitTypeCast(node, p);
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree node, Void p) {
        // TODO Is it worth adding a separate annotation for this?
        return super.visitInstanceOf(node, p);
    }

    @Override
    public Void visitModifiers(ModifiersTree node, Void p) {
        if (node!=null && modifiers!=null) {
            for (Modifier hasmod : node.getFlags()) {
                for (String searchmod : modifiers) {
                    if (hasmod.toString().equals(searchmod)) {
                        checker.report(Result.failure("Modifier." + hasmod), node);
                    }
                }
            }
        }
        return super.visitModifiers(node, p);
    }
}
