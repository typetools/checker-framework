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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.JsrInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.RET;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.ReturnaddressType;
import org.apache.bcel.generic.Type;
import org.apache.bcel.verifier.VerificationResult;
import org.apache.bcel.verifier.exc.AssertionViolatedException;
import org.apache.bcel.verifier.exc.VerifierConstraintViolatedException;
import org.apache.bcel.verifier.structurals.ControlFlowGraph;
import org.apache.bcel.verifier.structurals.ExceptionHandler;
import org.apache.bcel.verifier.structurals.ExecutionVisitor;
import org.apache.bcel.verifier.structurals.Frame;
import org.apache.bcel.verifier.structurals.InstConstraintVisitor;
import org.apache.bcel.verifier.structurals.InstructionContext;
import org.apache.bcel.verifier.structurals.LocalVariables;
import org.apache.bcel.verifier.structurals.OperandStack;
import org.apache.bcel.verifier.structurals.UninitializedObjectType;


/**
 * This is a slightly modified version of Pass3bVerifier from BCEL.
 * It uses LimitedConstaintVisitor rather than InstConstraintVisitor
 * to implement the constraints.  The LimitedConstraintVisitor doesn't
 * do any checking outside of the current class and removes some checks
 * so that this will pass on the JDK.  This version also provides the
 * ability to get the contents of the stack for each instruction in
 * the method.
 *
 * This PassVerifier verifies a method of class file according to pass 3,
 * so-called structural verification as described in The Java Virtual Machine
 * Specification, 2nd edition.
 * More detailed information is to be found at the do_verify() method's
 * documentation.
 *
 * @version $Id: StackVer.java,v 1.4 2005-11-01 19:13:00 jhp Exp $
 * @author <A HREF="http://www.inf.fu-berlin.de/~ehaase"/>Enver Haase</A>
 * @see #get_stack_types()
 */
public final class StackVer {
	/* TODO:	Throughout pass 3b, upper halves of LONG and DOUBLE
						are represented by Type.UNKNOWN. This should be changed
						in favour of LONG_Upper and DOUBLE_Upper as in pass 2. */

	/**
	 * An InstructionContextQueue is a utility class that holds
	 * (InstructionContext, ArrayList) pairs in a Queue data structure.
	 * This is used to hold information about InstructionContext objects
	 * externally --- i.e. that information is not saved inside the
	 * InstructionContext object itself. This is useful to save the
	 * execution path of the symbolic execution of the
	 * Pass3bVerifier - this is not information
	 * that belongs into the InstructionContext object itself.
	 * Only at "execute()"ing
	 * time, an InstructionContext object will get the current information
	 * we have about its symbolic execution predecessors.
	 */
	private static final class InstructionContextQueue{
		private Vector<InstructionContext> ics = new Vector<InstructionContext>();
		private Vector<ArrayList<InstructionContext>> ecs = new Vector<ArrayList<InstructionContext>>();
		/**
		 * TODO
		 * @param ic
		 * @param executionChain
		 */
		public void add(InstructionContext ic, ArrayList<InstructionContext> executionChain){
			ics.add(ic);
			ecs.add(executionChain);
		}
		/**
		 * TODO
		 * @return
		 */
		public boolean isEmpty(){
			return ics.isEmpty();
		}
		/**
		 * TODO
		 */
		public void remove(){
			this.remove(0);
		}
		/**
		 * TODO
		 * @param i
		 */
		public void remove(int i){
			ics.remove(i);
			ecs.remove(i);
		}
		/**
		 * TODO
		 * @param i
		 * @return
		 */
		public InstructionContext getIC(int i){
			return ics.get(i);
		}
		/**
		 * TODO
		 * @param i
		 * @return
		 */
		public ArrayList<InstructionContext> getEC(int i){
			return ecs.get(i);
		}
		/**
		 * TODO
		 * @return
		 */
		public int size(){
			return ics.size();
		}
	} // end Inner Class InstructionContextQueue

//    /**
//     * This modified version of InstContraintVisitor causes the verifier
//     * not to load any other classes as part of verification (causing it
//     * to presume that information in this class is correct.  This is
//     * necessary for efficiency and also prevents some other problems
//     */
//    private static class MyLimitedConstraintVisitor
//      extends InstConstraintVisitor {
//
//	  public void visitLoadClass(LoadClass o){
//          // System.out.println ("Skipping visitLoadClass " + o);
//      }
//	  public void visitINVOKEVIRTUAL(INVOKEVIRTUAL o){
//          // TODO JHP:  This should really check the arguments (for when
//          // we use this code as a verifier)
//          // System.out.println ("Skipping invoke virtual " + o);
//      }
//
//      public void visitNEW (NEW o) {
//          // All this does is make sure that the new object is as expected.
//          // It fails if it can't find it, which we would prefer it not
//          // to do.
//      }
//
//      public void visitLDC(LDC o){
//          // Skipping check because LDC has new capabilities in 1.5 not
//          // supported by this check (it allows constant classes in addition
//          // to strings, integers, and floats
//      }
//
//      public void visitLDC_W(LDC_W o){
//          // Skipping check because LDC has new capabilities in 1.5 not
//          // supported by this check (it allows constant classes in addition
//          // to strings, integers, and floats
//      }
//    }

	/** In DEBUG mode, the verification algorithm is not randomized. */
	private static final boolean DEBUG = true;

	/** The Verifier that created this. */
	// private Verifier myOwner;

    /** The types on the stack for each instruction by byte code offset **/
    private StackTypes stack_types;

	/**
	 * This class should only be instantiated by a Verifier.
	 *
	 * @see org.apache.bcel.verifier.Verifier
	 */
	public StackVer (){
	}

   /**
    * Return the types on the stack at each byte code offset.  Only valid
    * after do_stack_ver() is called
    */
    public StackTypes get_stack_types () {
      return (stack_types);
    }

	/**
	 * Whenever the outgoing frame
	 * situation of an InstructionContext changes, all its successors are
	 * put [back] into the queue [as if they were unvisited].
   * The proof of termination is about the existence of a
   * fix point of frame merging.
	 */
	private void circulationPump(ControlFlowGraph cfg, InstructionContext start, Frame vanillaFrame, InstConstraintVisitor icv, ExecutionVisitor ev){
		final Random random = new Random();
		InstructionContextQueue icq = new InstructionContextQueue();

        stack_types.set (start.getInstruction().getPosition(), vanillaFrame);
		start.execute(vanillaFrame, new ArrayList(), icv, ev);	// new ArrayList() <=>	no Instruction was executed before
																									//									=> Top-Level routine (no jsr call before)
		icq.add(start, new ArrayList<InstructionContext>());

		// LOOP!
		while (!icq.isEmpty()){
			InstructionContext u;
			ArrayList<InstructionContext> ec;
			if (!DEBUG){
				int r = random.nextInt(icq.size());
				u = icq.getIC(r);
				ec = icq.getEC(r);
				icq.remove(r);
			}
			else{
				u  = icq.getIC(0);
				ec = icq.getEC(0);
				icq.remove(0);
			}

			ArrayList<?> oldchain = (ArrayList<?>) (ec.clone());
      // this makes Java 5.0 grumpy
			// ArrayList<InstructionContext> newchain = (ArrayList) (ec.clone());
      ArrayList<InstructionContext> newchain = new ArrayList<InstructionContext>(ec);
			newchain.add(u);

			if ((u.getInstruction().getInstruction()) instanceof RET){
				// We can only follow _one_ successor, the one after the
				// JSR that was recently executed.
				RET ret = (RET) (u.getInstruction().getInstruction());
				ReturnaddressType t = (ReturnaddressType) u.getOutFrame(oldchain).getLocals().get(ret.getIndex());
				InstructionContext theSuccessor = cfg.contextOf(t.getTarget());

				// Sanity check
				InstructionContext lastJSR = null;
				int skip_jsr = 0;
				for (int ss=oldchain.size()-1; ss >= 0; ss--){
					if (skip_jsr < 0){
						throw new AssertionViolatedException("More RET than JSR in execution chain?!");
					}
//System.err.println("+"+oldchain.get(ss));
					if (((InstructionContext) oldchain.get(ss)).getInstruction().getInstruction() instanceof JsrInstruction){
						if (skip_jsr == 0){
							lastJSR = (InstructionContext) oldchain.get(ss);
							break;
						}
            skip_jsr--;
					}
					if (((InstructionContext) oldchain.get(ss)).getInstruction().getInstruction() instanceof RET){
						skip_jsr++;
					}
				}
				if (lastJSR == null){
					throw new AssertionViolatedException("RET without a JSR before in ExecutionChain?! EC: '"+oldchain+"'.");
				}
				JsrInstruction jsr = (JsrInstruction) (lastJSR.getInstruction().getInstruction());
				if ( theSuccessor != (cfg.contextOf(jsr.physicalSuccessor())) ){
					throw new AssertionViolatedException("RET '"+u.getInstruction()+"' info inconsistent: jump back to '"+theSuccessor+"' or '"+cfg.contextOf(jsr.physicalSuccessor())+"'?");
				}

                Frame f = u.getOutFrame(oldchain);
                stack_types.set (theSuccessor.getInstruction().getPosition(),
                                  f);
				if (theSuccessor.execute(f, newchain, icv, ev)){
          // This makes 5.0 grumpy: icq.add(theSuccessor, (ArrayList) newchain.clone());
          icq.add(theSuccessor, new ArrayList<InstructionContext>(newchain));
				}
			}
			else{// "not a ret"

				// Normal successors. Add them to the queue of successors.
				InstructionContext[] succs = u.getSuccessors();
				for (int s=0; s<succs.length; s++){
					InstructionContext v = succs[s];
                    Frame f = u.getOutFrame(oldchain);
                    stack_types.set (v.getInstruction().getPosition(), f);
					if (v.execute(f, newchain, icv, ev)){
            // This makes 5.0 grumpy: icq.add(v, (ArrayList) newchain.clone());
            icq.add(v, new ArrayList<InstructionContext>(newchain));
					}
				}
			}// end "not a ret"

			// Exception Handlers. Add them to the queue of successors.
			// [subroutines are never protected; mandated by JustIce]
			ExceptionHandler[] exc_hds = u.getExceptionHandlers();
			for (int s=0; s<exc_hds.length; s++){
				InstructionContext v = cfg.contextOf(exc_hds[s].getHandlerStart());
				// TODO: the "oldchain" and "newchain" is used to determine the subroutine
				// we're in (by searching for the last JSR) by the InstructionContext
				// implementation. Therefore, we should not use this chain mechanism
				// when dealing with exception handlers.
				// Example: a JSR with an exception handler as its successor does not
				// mean we're in a subroutine if we go to the exception handler.
				// We should address this problem later; by now we simply "cut" the chain
				// by using an empty chain for the exception handlers.
				//if (v.execute(new Frame(u.getOutFrame(oldchain).getLocals(), new OperandStack (u.getOutFrame().getStack().maxStack(), (exc_hds[s].getExceptionType()==null? Type.THROWABLE : exc_hds[s].getExceptionType())) ), newchain), icv, ev){
					//icq.add(v, (ArrayList) newchain.clone());
                Frame f = new Frame(u.getOutFrame(oldchain).getLocals(),
                  new OperandStack (u.getOutFrame(oldchain).getStack()
                    .maxStack(),
                  (exc_hds[s].getExceptionType()==null
                    ? Type.THROWABLE : exc_hds[s].getExceptionType())) );
                stack_types.set (v.getInstruction().getPosition(), f);
				if (v.execute(f, new ArrayList(), icv, ev)){
					icq.add(v, new ArrayList<InstructionContext>());
				}
			}

		}// while (!icq.isEmpty()) END

		InstructionHandle ih = start.getInstruction();
		do{
			if ((ih.getInstruction() instanceof ReturnInstruction) && (!(cfg.isDead(ih)))) {
				InstructionContext ic = cfg.contextOf(ih);
				Frame f = ic.getOutFrame(new ArrayList()); // TODO: This is buggy, we check only the top-level return instructions this way. Maybe some maniac returns from a method when in a subroutine?
				LocalVariables lvs = f.getLocals();
				for (int i=0; i<lvs.maxLocals(); i++){
					if (lvs.get(i) instanceof UninitializedObjectType){
						this.addMessage("Warning: ReturnInstruction '"+ic+"' may leave method with an uninitialized object in the local variables array '"+lvs+"'.");
					}
				}
				OperandStack os = f.getStack();
				for (int i=0; i<os.size(); i++){
					if (os.peek(i) instanceof UninitializedObjectType){
						this.addMessage("Warning: ReturnInstruction '"+ic+"' may leave method with an uninitialized object on the operand stack '"+os+"'.");
					}
				}
			}
		}while ((ih = ih.getNext()) != null);

 	}

	/**
	 * Implements the pass 3b data flow analysis as described in the
	 * Java Virtual Machine Specification, Second Edition.  As it is doing
   * so it keeps track of the stack and local variables at each instruction.
 	 *
 	 * @see org.apache.bcel.verifier.statics.Pass2Verifier#getLocalVariablesInfo(int)
 	 */
	public VerificationResult do_stack_ver (MethodGen mg){

      /*
        if (! myOwner.doPass3a(method_no).equals(VerificationResult.VR_OK)){
			return VerificationResult.VR_NOTYET;
		}
      */
		// Pass 3a ran before, so it's safe to assume the JavaClass object is
		// in the BCEL repository.
		// JavaClass jc = Repository.lookupClass(myOwner.getClassName());

        ConstantPoolGen constantPoolGen = mg.getConstantPool();
		// Init Visitors
		InstConstraintVisitor icv = new LimitedConstraintVisitor();
		icv.setConstantPoolGen(constantPoolGen);

		ExecutionVisitor ev = new ExecutionVisitor();
		ev.setConstantPoolGen(constantPoolGen);

		try{
            stack_types = new StackTypes (mg);

			icv.setMethodGen(mg);

			////////////// DFA BEGINS HERE ////////////////
			if (! (mg.isAbstract() || mg.isNative()) ){ // IF mg HAS CODE (See pass 2)

				ControlFlowGraph cfg = new ControlFlowGraph(mg);

				// Build the initial frame situation for this method.
				Frame f = new Frame(mg.getMaxLocals(),mg.getMaxStack());
				if ( !mg.isStatic() ){
					if (mg.getName().equals(Constants.CONSTRUCTOR_NAME)){
						Frame._this = new UninitializedObjectType(new ObjectType(mg.getClassName()));
						f.getLocals().set(0, Frame._this);
					}
					else{
						Frame._this = null;
						f.getLocals().set(0, new ObjectType(mg.getClassName()));
					}
				}
				Type[] argtypes = mg.getArgumentTypes();
				int twoslotoffset = 0;
				for (int j=0; j<argtypes.length; j++){
					if (argtypes[j] == Type.SHORT || argtypes[j] == Type.BYTE || argtypes[j] == Type.CHAR || argtypes[j] == Type.BOOLEAN){
						argtypes[j] = Type.INT;
					}
					f.getLocals().set(twoslotoffset + j + (mg.isStatic()?0:1), argtypes[j]);
					if (argtypes[j].getSize() == 2){
						twoslotoffset++;
						f.getLocals().set(twoslotoffset + j + (mg.isStatic()?0:1), Type.UNKNOWN);
					}
				}
				circulationPump(cfg, cfg.contextOf(mg.getInstructionList().getStart()), f, icv, ev);
			}
		}
		catch (VerifierConstraintViolatedException ce){
			ce.extendMessage("Constraint violated in method '"+mg+"':\n","");
			return new VerificationResult(VerificationResult.VERIFIED_REJECTED, ce.getMessage());
		}
		catch (RuntimeException re){
			// These are internal errors

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			re.printStackTrace(pw);

			throw new AssertionViolatedException("Some RuntimeException occured while verify()ing class '"+mg.getClassName()+"', method '"+mg+"'. Original RuntimeException's stack trace:\n---\n"+sw+"---\n");
		}
		return VerificationResult.VR_OK;
	}

  // Code from PassVerifier in BCEL so that we don't have to extend it

	/** The (warning) messages. */
	private ArrayList<String> messages = new ArrayList<String>(); //Type of elements: String

	/**
	 * This method adds a (warning) message to the message pool of this
	 * PassVerifier. This method is normally only internally used by
	 * BCEL's class file verifier "JustIce" and should not be used from
	 * the outside.
	 */
	public void addMessage(String message){
		messages.add(message);
	}


}

// Local Variables:
// tab-width: 2
// End:
