package checkers.igj;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.CompilationUnitTree;

import checkers.source.*;
import checkers.types.*;
import checkers.util.TypesUtils;

/**
 * An annotation processor that checks a program use of IGJ mutability
 * type annotations, as specified by the FSE 2007 paper, ({@code @ReadOnly},
 * {@code @Mutable}, and {@Code @Immutable}).
 * 
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes( { "checkers.igj.quals.*" })
public class IGJChecker extends SourceChecker {

    @Override
    protected Properties getMessages(String fileName) throws IOException {
        // Use a message file if one exists.
        if (new File(fileName).exists()) {
            String path = System.getProperty("messages", ".");
            Properties p = new Properties();
            p.load(new FileInputStream(path + File.separator + fileName));
            return p;
        }

        // Sets message defaults.
        Properties msgDefaults = new Properties();
        msgDefaults.put("assignment.invalid", "incompatible types.\nfound   : %s\nrequired: %s");
        msgDefaults.put("assignment.invalid.field", "Cannot re-assign a field of an immutable or readonly object");
        msgDefaults.put("assignment.invalid.ROMethod", "ReadOnly and Immutable methods cannot mutate fields");

        msgDefaults.put("type.invalid","Invalid IGJ type for given class/object.\nfound   : %s\nrequired: %s");
        msgDefaults.put("readonly.type.invalid", "Cannot annotate class with @ReadOnly");
        msgDefaults.put("methodinvocation.invalid","Cannot invoke a %s method from a %s method.");
        msgDefaults.put("param.invalid", "Invalid Param for required method.\nfound   : %s\nrequired: %s");
        msgDefaults.put("cast.invalid", "Cannot cast expression to given type");
        msgDefaults.put("method.receiver.invalid", "invalid type for receiver");

        msgDefaults.put("override.return.invalid", 
                "%s in %s cannot override %s in %s; attempting to use incompatable return type.\nfound   : %s\nrequired: %s");
        msgDefaults.put("override.param.invalid", 
                "%s in %s cannot override %s in %s; attempting to use incompatable parameter type.\nfound   : %s\nrequired: %s");
        msgDefaults.put("override.receiver.invalid", 
                "%s in %s cannot override %s in %s; attempting to use incompatable return type.\nfound   : %s\nrequired: %s");
        
        msgDefaults.put("@AssignsFields", "@ReadOnly");
        return msgDefaults;
    }

    @Override
    protected IGJVisitor getSourceVisitor(CompilationUnitTree root) {
        return new IGJVisitor(this, root);
    }

    @Override
    public AnnotatedTypeFactory getFactory(ProcessingEnvironment env,
            CompilationUnitTree root) {
        return new IGJAnnotatedTypeFactory(env, root);
    }

    protected boolean shouldSkip(AnnotatedMethodType method) {
       TypeElement element = (TypeElement)method.getElement().getEnclosingElement();
       String str = element.getQualifiedName().toString();
       return shouldSkip(str);
    }
    
    protected boolean shouldSkip(AnnotatedClassType type) {
        assert type.getUnderlyingType() != null;
        TypeMirror typeMirror = type.getUnderlyingType();
        if (typeMirror.getKind() == TypeKind.ARRAY)
            typeMirror = TypesUtils.getDeepComponent((ArrayType)typeMirror);
        if (typeMirror.getKind() != TypeKind.DECLARED)
            return false;
        
        String typeName = 
            TypesUtils.getQualifiedName((DeclaredType)typeMirror).toString();

        return shouldSkip(typeName);
    }
}
