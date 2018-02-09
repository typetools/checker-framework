/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package javax.script;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Extended by classes that store results of compilations.  State
 * might be stored in the form of Java classes, Java class files or scripting
 * language opcodes.  The script may be executed repeatedly
 * without reparsing.
 * <br><br>
 * Each <code>CompiledScript</code> is associated with a <code>ScriptEngine</code> -- A call to an  <code>eval</code>
 * method of the <code>CompiledScript</code> causes the execution of the script by the
 * <code>ScriptEngine</code>.  Changes in the state of the <code>ScriptEngine</code> caused by execution
 * of the <code>CompiledScript</code>  may visible during subsequent executions of scripts by the engine.
 *
 * @author Mike Grogan
 * @since 1.6
 */
public abstract class CompiledScript {

    /**
     * Executes the program stored in this <code>CompiledScript</code> object.
     *
     * @param context A <code>ScriptContext</code> that is used in the same way as
     * the <code>ScriptContext</code> passed to the <code>eval</code> methods of
     * <code>ScriptEngine</code>.
     *
     * @return The value returned by the script execution, if any.  Should return <code>null</code>
     * if no value is returned by the script execution.
     *
     * @throws ScriptException if an error occurs.
     * @throws NullPointerException if context is null.
     */

    public abstract @Nullable Object eval(ScriptContext context) throws ScriptException;

    /**
     * Executes the program stored in the <code>CompiledScript</code> object using
     * the supplied <code>Bindings</code> of attributes as the <code>ENGINE_SCOPE</code> of the
     * associated <code>ScriptEngine</code> during script execution.  If bindings is null,
     * then the effect of calling this method is same as that of eval(getEngine().getContext()).
     * <p>.
     * The <code>GLOBAL_SCOPE</code> <code>Bindings</code>, <code>Reader</code> and <code>Writer</code>
     * associated with the default <code>ScriptContext</code> of the associated <code>ScriptEngine</code> are used.
     *
     * @param bindings The bindings of attributes used for the <code>ENGINE_SCOPE</code>.
     *
     * @return The return value from the script execution
     *
     * @throws ScriptException if an error occurs.
     */
    @SuppressWarnings("nullness")
    public Object eval(@Nullable Bindings bindings) throws ScriptException {

        ScriptContext ctxt = getEngine().getContext();

        if (bindings != null) {
            SimpleScriptContext tempctxt = new SimpleScriptContext();
            tempctxt.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            tempctxt.setBindings(ctxt.getBindings(ScriptContext.GLOBAL_SCOPE),
                    ScriptContext.GLOBAL_SCOPE);
            tempctxt.setWriter(ctxt.getWriter());
            tempctxt.setReader(ctxt.getReader());
            tempctxt.setErrorWriter(ctxt.getErrorWriter());
            ctxt = tempctxt;
        }

        return eval(ctxt);
    }


    /**
     * Executes the program stored in the <code>CompiledScript</code> object.  The
     * default <code>ScriptContext</code> of the associated <code>ScriptEngine</code> is used.
     * The effect of calling this method is same as that of eval(getEngine().getContext()).
     *
     * @return The return value from the script execution
     *
     * @throws ScriptException if an error occurs.
     */
    @SuppressWarnings("nullness")
    public Object eval() throws ScriptException {
        return eval(getEngine().getContext());
    }

    /**
     * Returns the <code>ScriptEngine</code> whose <code>compile</code> method created this <code>CompiledScript</code>.
     * The <code>CompiledScript</code> will execute in this engine.
     *
     * @return The <code>ScriptEngine</code> that created this <code>CompiledScript</code>
     */
    public abstract ScriptEngine getEngine();

}
