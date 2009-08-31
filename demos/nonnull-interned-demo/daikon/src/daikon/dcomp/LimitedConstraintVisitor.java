package daikon.dcomp;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache BCEL" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache BCEL", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import org.apache.bcel.generic.*;
import org.apache.bcel.verifier.structurals.Frame;
import org.apache.bcel.verifier.structurals.InstConstraintVisitor;
import org.apache.bcel.verifier.structurals.LocalVariables;
import org.apache.bcel.verifier.structurals.OperandStack;


/**
 * A Visitor class testing for valid preconditions of JVM instructions.
 * No checks are actually implemented.  This overrides the class in BCEL
 * which incorrectly fails on many valid class files.
 */
public class LimitedConstraintVisitor extends InstConstraintVisitor {

  // private static ObjectType GENERIC_ARRAY = new ObjectType("org.apache.bcel.verifier.structurals.GenericArray");

	/**
	 * The constructor. Constructs a new instance of this class.
	 */
	public LimitedConstraintVisitor() {}

	/**
	 * The Execution Frame we're working on.
	 *
	 * @see #setFrame(Frame f)
	 * @see #locals()
	 * @see #stack()
	 */
	private Frame frame = null;

	/**
	 * The ConstantPoolGen we're working on.
	 *
	 * @see #setConstantPoolGen(ConstantPoolGen cpg)
	 */
	// private ConstantPoolGen cpg = null;

	/**
	 * The MethodGen we're working on.
	 *
	 * @see #setMethodGen(MethodGen mg)
	 */
	// private MethodGen mg = null;

	/**
	 * The OperandStack we're working on.
	 *
	 * @see #setFrame(Frame f)
	 */
	private OperandStack stack() {
		return frame.getStack();
	}

	/**
	 * The LocalVariables we're working on.
	 *
	 * @see #setFrame(Frame f)
	 */
	private LocalVariables locals() {
		return frame.getLocals();
	}

	/**
   * This method is called by the visitXXX() to notify the acceptor of this InstConstraintVisitor
   * that a constraint violation has occured. This is done by throwing an instance of a
   * StructuralCodeConstraintException.
   * @throws org.apache.bcel.verifier.exc.StructuralCodeConstraintException always.
   */
//	private void constraintViolated(Instruction violator, String description) {
//		String fq_classname = violator.getClass().getName();
//		throw new StructuralCodeConstraintException("Instruction "+ fq_classname.substring(fq_classname.lastIndexOf('.')+1) +" constraint violated: " + description);
//	}

	/**
	 * This returns the single instance of the InstConstraintVisitor class.
	 * To operate correctly, other values must have been set before actually
	 * using the instance.
	 * Use this method for performance reasons.
	 *
	 * @see #setConstantPoolGen(ConstantPoolGen cpg)
	 * @see #setMethodGen(MethodGen mg)
	 */
	public void setFrame(Frame f) {
		this.frame = f;
		//if (singleInstance.mg == null || singleInstance.cpg == null) throw new AssertionViolatedException("Forgot to set important values first.");
	}

	/**
	 * Sets the ConstantPoolGen instance needed for constraint
	 * checking prior to execution.
	 */
	public void setConstantPoolGen(ConstantPoolGen cpg) {
		// this.cpg = cpg;
	}

	/**
	 * Sets the MethodGen instance needed for constraint
	 * checking prior to execution.
	 */
	public void setMethodGen(MethodGen mg) {
		// this.mg = mg;
	}

//	/**
//	 * Assures index is of type INT.
//	 * @throws org.apache.bcel.verifier.exc.StructuralCodeConstraintException if the above constraint is not satisfied.
//	 */
//	private void indexOfInt(Instruction o, Type index) {
//		if (! index.equals(Type.INT))
//				constraintViolated(o, "The 'index' is not of type int but of type "+index+".");
//	}
//
//	/**
//	 * Assures the ReferenceType r is initialized (or Type.NULL).
//	 * Formally, this means (!(r instanceof UninitializedObjectType)), because
//	 * there are no uninitialized array types.
//	 * @throws org.apache.bcel.verifier.exc.StructuralCodeConstraintException if the above constraint is not satisfied.
//	 */
//	private void referenceTypeIsInitialized(Instruction o, ReferenceType r) {
//		if (r instanceof UninitializedObjectType) {
//			constraintViolated(o, "Working on an uninitialized object '"+r+"'.");
//		}
//	}
//
//	/** Assures value is of type INT. */
//	private void valueOfInt(Instruction o, Type value) {
//		if (! value.equals(Type.INT))
//				constraintViolated(o, "The 'value' is not of type int but of type "+value+".");
//	}
//
//	/**
//	 * Assures arrayref is of ArrayType or NULL;
//	 * returns true if and only if arrayref is non-NULL.
//	 * @throws org.apache.bcel.verifier.exc.StructuralCodeConstraintException if the above constraint is violated.
// 	 */
//	private boolean arrayrefOfArrayType(Instruction o, Type arrayref) {
//		if (! ((arrayref instanceof ArrayType) || arrayref.equals(Type.NULL)) )
//				constraintViolated(o, "The 'arrayref' does not refer to an array but is of type "+arrayref+".");
//		return (arrayref instanceof ArrayType);
//	}

	/***************************************************************/
	/* MISC                                                        */
	/***************************************************************/
	/**
	 * Ensures the general preconditions of an instruction that accesses the stack.
	 * This method is here because BCEL has no such superinterface for the stack
	 * accessing instructions; and there are funny unexpected exceptions in the
	 * semantices of the superinterfaces and superclasses provided.
	 * E.g. SWAP is a StackConsumer, but DUP_X1 is not a StackProducer.
	 * Therefore, this method is called by all StackProducer, StackConsumer,
	 * and StackInstruction instances via their visitXXX() method.
	 * Unfortunately, as the superclasses and superinterfaces overlap, some instructions
	 * cause this method to be called two or three times. [TODO: Fix this.]
	 *
	 * @see #visitStackConsumer(StackConsumer o)
	 * @see #visitStackProducer(StackProducer o)
	 * @see #visitStackInstruction(StackInstruction o)
	 */
//	private void _visitStackAccessor(Instruction o) {
//        // System.out.println ("visitStackAccessor: " + o);
//		int consume = o.consumeStack(cpg); // Stack values are always consumed first; then produced.
//		if (consume > stack().slotsUsed()) {
//			constraintViolated((Instruction) o, "Cannot consume "+consume+" stack slots: only "+stack().slotsUsed()+" slot(s) left on stack!\nStack:\n"+stack());
//		}
//
//		int produce = o.produceStack(cpg) - ((Instruction) o).consumeStack(cpg); // Stack values are always consumed first; then produced.
//		if ( produce + stack().slotsUsed() > stack().maxStack() ) {
//			constraintViolated((Instruction) o, "Cannot produce "+produce+" stack slots: only "+(stack().maxStack()-stack().slotsUsed())+" free stack slot(s) left.\nStack:\n"+stack());
//		}
//	}

	/***************************************************************/
	/* "generic"visitXXXX methods where XXXX is an interface       */
	/* therefore, we don't know the order of visiting; but we know */
	/* these methods are called before the visitYYYY methods below */
	/***************************************************************/

	/**
	 * Assures the generic preconditions of a LoadClass instance.
	 * The referenced class is loaded and pass2-verified.
	 */
  public void visitLoadClass(LoadClass o) {
  }

	/**
	 * Ensures the general preconditions of a StackConsumer instance.
	 */
	public void visitStackConsumer(StackConsumer o) {
	}

	/**
	 * Ensures the general preconditions of a StackProducer instance.
	 */
	public void visitStackProducer(StackProducer o) {
	}


	/***************************************************************/
	/* "generic" visitYYYY methods where YYYY is a superclass.     */
	/* therefore, we know the order of visiting; we know           */
	/* these methods are called after the visitXXXX methods above. */
	/***************************************************************/
	/**
	 * Ensures the general preconditions of a CPInstruction instance.
	 */
	public void visitCPInstruction(CPInstruction o) {
	}

	/**
	 * Ensures the general preconditions of a FieldInstruction instance.
	 */
	 public void visitFieldInstruction(FieldInstruction o) {
	 }

	/**
	 * Ensures the general preconditions of an InvokeInstruction instance.
	 */
	 public void visitInvokeInstruction(InvokeInstruction o) {
	 }

	/**
	 * Ensures the general preconditions of a StackInstruction instance.
	 */
	public void visitStackInstruction(StackInstruction o) {
	}

	/**
	 * Assures the generic preconditions of a LocalVariableInstruction instance.
	 * That is, the index of the local variable must be valid.
	 */
	public void visitLocalVariableInstruction(LocalVariableInstruction o) {
	}

	/**
	 * Assures the generic preconditions of a LoadInstruction instance.
	 */
	public void visitLoadInstruction(LoadInstruction o) {
	}

	/**
	 * Assures the generic preconditions of a StoreInstruction instance.
	 */
	public void visitStoreInstruction(StoreInstruction o) {
	}

	/**
	 * Assures the generic preconditions of a ReturnInstruction instance.
	 */
	public void visitReturnInstruction(ReturnInstruction o) {
	}

	/***************************************************************/
	/* "special"visitXXXX methods for one type of instruction each */
	/***************************************************************/

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitAALOAD(AALOAD o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitAASTORE(AASTORE o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitACONST_NULL(ACONST_NULL o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitALOAD(ALOAD o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitANEWARRAY(ANEWARRAY o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitARETURN(ARETURN o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitARRAYLENGTH(ARRAYLENGTH o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitASTORE(ASTORE o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitATHROW(ATHROW o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitBALOAD(BALOAD o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitBASTORE(BASTORE o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitBIPUSH(BIPUSH o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitBREAKPOINT(BREAKPOINT o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitCALOAD(CALOAD o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitCASTORE(CASTORE o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitCHECKCAST(CHECKCAST o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitD2F(D2F o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitD2I(D2I o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitD2L(D2L o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitDADD(DADD o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitDALOAD(DALOAD o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitDASTORE(DASTORE o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitDCMPG(DCMPG o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitDCMPL(DCMPL o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitDCONST(DCONST o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitDDIV(DDIV o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitDLOAD(DLOAD o) {
	}

	/**
	 * Ensures the specific preconditions of the said instruction.
	 */
	public void visitDMUL(DMUL o) {
	}

	public void visitDNEG(DNEG o) {}
	public void visitDREM(DREM o) {}
	public void visitDRETURN(DRETURN o) {}
	public void visitDSTORE(DSTORE o) {}
	public void visitDSUB(DSUB o) {}
	public void visitDUP(DUP o) {}
	public void visitDUP_X1(DUP_X1 o) {}
	public void visitDUP_X2(DUP_X2 o) {}
	public void visitDUP2(DUP2 o) {}
	public void visitDUP2_X1(DUP2_X1 o) {}
	public void visitDUP2_X2(DUP2_X2 o) {}
	public void visitF2D(F2D o) {}
	public void visitF2I(F2I o) {}
	public void visitF2L(F2L o) {}
	public void visitFADD(FADD o) {}
	public void visitFALOAD(FALOAD o) {}
	public void visitFASTORE(FASTORE o) {}
	public void visitFCMPG(FCMPG o) {}
	public void visitFCMPL(FCMPL o) {}
	public void visitFCONST(FCONST o) {}
	public void visitFDIV(FDIV o) {}
	public void visitFLOAD(FLOAD o) {}
	public void visitFMUL(FMUL o) {}
	public void visitFNEG(FNEG o) {}
	public void visitFREM(FREM o) {}
	public void visitFRETURN(FRETURN o) {}
	public void visitFSTORE(FSTORE o) {}
	public void visitFSUB(FSUB o) {}
	public void visitGETFIELD(GETFIELD o) {}
	public void visitGETSTATIC(GETSTATIC o) {}
	public void visitGOTO(GOTO o) {}
	public void visitGOTO_W(GOTO_W o) {}
	public void visitI2B(I2B o) {}
	public void visitI2C(I2C o) {}
	public void visitI2D(I2D o) {}
	public void visitI2F(I2F o) {}
	public void visitI2L(I2L o) {}
	public void visitI2S(I2S o) {}
	public void visitIADD(IADD o) {}
	public void visitIALOAD(IALOAD o) {}
	public void visitIAND(IAND o) {}
	public void visitIASTORE(IASTORE o) {}
	public void visitICONST(ICONST o) {}
	public void visitIDIV(IDIV o) {}
	public void visitIF_ACMPEQ(IF_ACMPEQ o) {}
	public void visitIF_ACMPNE(IF_ACMPNE o) {}
	public void visitIF_ICMPEQ(IF_ICMPEQ o) {}
	public void visitIF_ICMPGE(IF_ICMPGE o) {}
	public void visitIF_ICMPGT(IF_ICMPGT o) {}
	public void visitIF_ICMPLE(IF_ICMPLE o) {}
	public void visitIF_ICMPLT(IF_ICMPLT o) {}
	public void visitIF_ICMPNE(IF_ICMPNE o) {}
	public void visitIFEQ(IFEQ o) {}
	public void visitIFGE(IFGE o) {}
	public void visitIFGT(IFGT o) {}
	public void visitIFLE(IFLE o) {}
	public void visitIFLT(IFLT o) {}
	public void visitIFNE(IFNE o) {}
	public void visitIFNONNULL(IFNONNULL o) {}
	public void visitIFNULL(IFNULL o) {}
	public void visitIINC(IINC o) {}
	public void visitILOAD(ILOAD o) {}
	public void visitIMPDEP1(IMPDEP1 o) {}
	public void visitIMPDEP2(IMPDEP2 o) {}
	public void visitIMUL(IMUL o) {}
	public void visitINEG(INEG o) {}
	public void visitINSTANCEOF(INSTANCEOF o) {}
	public void visitINVOKEINTERFACE(INVOKEINTERFACE o) {}
	public void visitINVOKESPECIAL(INVOKESPECIAL o) {}
	public void visitINVOKESTATIC(INVOKESTATIC o) {}
	public void visitINVOKEVIRTUAL(INVOKEVIRTUAL o) {}
	public void visitIOR(IOR o) {}
	public void visitIREM(IREM o) {}
	public void visitIRETURN(IRETURN o) {}
	public void visitISHL(ISHL o) {}
	public void visitISHR(ISHR o) {}
	public void visitISTORE(ISTORE o) {}
	public void visitISUB(ISUB o) {}
	public void visitIUSHR(IUSHR o) {}
	public void visitIXOR(IXOR o) {}
	public void visitJSR(JSR o) {}
	public void visitJSR_W(JSR_W o) {}
	public void visitL2D(L2D o) {}
	public void visitL2F(L2F o) {}
	public void visitL2I(L2I o) {}
	public void visitLADD(LADD o) {}
	public void visitLALOAD(LALOAD o) {}
	public void visitLAND(LAND o) {}
	public void visitLASTORE(LASTORE o) {}
	public void visitLCMP(LCMP o) {}
	public void visitLCONST(LCONST o) {}
	public void visitLDC(LDC o) {}
	public void visitLDC_W(LDC_W o) {}
	public void visitLDC2_W(LDC2_W o) {}
	public void visitLDIV(LDIV o) {}
	public void visitLLOAD(LLOAD o) {}
	public void visitLMUL(LMUL o) {}
	public void visitLNEG(LNEG o) {}
	public void visitLOOKUPSWITCH(LOOKUPSWITCH o) {}
	public void visitLOR(LOR o) {}
	public void visitLREM(LREM o) {}
	public void visitLRETURN(LRETURN o) {}
	public void visitLSHL(LSHL o) {}
	public void visitLSHR(LSHR o) {}
	public void visitLSTORE(LSTORE o) {}
	public void visitLSUB(LSUB o) {}
	public void visitLUSHR(LUSHR o) {}
	public void visitLXOR(LXOR o) {}
	public void visitMONITORENTER(MONITORENTER o) {}
	public void visitMONITOREXIT(MONITOREXIT o) {}
	public void visitMULTIANEWARRAY(MULTIANEWARRAY o) {}
	public void visitNEW(NEW o) {}
	public void visitNEWARRAY(NEWARRAY o) {}
	public void visitNOP(NOP o) {}
	public void visitPOP(POP o) {}
	public void visitPOP2(POP2 o) {}
	public void visitPUTFIELD(PUTFIELD o) {}
	public void visitPUTSTATIC(PUTSTATIC o) {}
	public void visitRET(RET o) {}
	public void visitRETURN(RETURN o) {}
	public void visitSALOAD(SALOAD o) {}
	public void visitSASTORE(SASTORE o) {}
	public void visitSIPUSH(SIPUSH o) {}
	public void visitSWAP(SWAP o) {}
	public void visitTABLESWITCH(TABLESWITCH o) {}
}

// Local Variables:
// tab-width: 2
// End:
