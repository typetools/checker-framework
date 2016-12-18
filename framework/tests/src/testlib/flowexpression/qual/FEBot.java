package testlib.flowexpression.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;

@SubtypeOf({FlowExp.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@ImplicitFor(literals = LiteralKind.NULL, types = TypeKind.VOID)
public @interface FEBot {}
