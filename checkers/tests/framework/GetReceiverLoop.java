import java.util.Collections;

class GetReceiverLoop {

    void test() {
        String s = Collections.emptyList().toString();
    }

    /*
     * getAnnotatedType( emptyList().toString )
     * -> TypeFromExpression.visitMemberSelect( emptyList().toString )
     * -> TypeFromExpression.visitMethodInvocation( emptyList() )
     * -> AnnotatedTypes.findTypeParameters( emptyList() )
     * -> AnnotatedTypes.assignedTo( emptyList() )
     * [the assignment context is emptyList().toString(), so then:]
     * -> AnnotatedTypeFactory.getReceiver( emptyList() )
     * -> getAnnotatedType( emtpyList() )
     * -> TypeFromExpression.visitMethodInvocation( emptyList() )
     * ...
     */


}