//
// $Id$

package com.threerings.bang.client.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.util.ShaderUniform;

import com.samskivert.util.ObjectUtil;

import com.threerings.jme.util.JmeUtil;

import com.threerings.bang.util.BasicContext;

/**
 * Caches shaders under their names and preprocessor definitions, ensuring that identical shaders
 * are only compiled once.
 */
public class ShaderCache
{
    public ShaderCache (BasicContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Creates a new shader state with the supplied vertex shader, fragment shader,
     * and preprocessor definitions.  If a program with the given parameters has already been
     * compiled, the state will use the program id of the existing shader.
     *
     * @return the newly created state, or <code>null</code> if there was an error and the
     * program could not be compiled.
     */
    public GLSLShaderObjectsState createShaderState (String vert, String frag, String... defs)
    {
        GLSLShaderObjectsState sstate = _ctx.getRenderer().createGLSLShaderObjectsState();
        return (configureShaderState(sstate, vert, frag, defs) ? sstate : null);
    }

    /**
     * (Re)configures an existing shader state with the supplied parameters.
     *
     * @return true if the shader was successfully configured, false if the shader could not
     * be compiled.
     */
    public boolean configureShaderState (
        GLSLShaderObjectsState sstate, String vert, String frag, String... defs)
    {
        ShaderKey key = new ShaderKey(vert, frag, defs);
        Integer programId = _programIds.get(key);
        if (programId == null) {
            GLSLShaderObjectsState pstate = JmeUtil.loadShaders(
                _ctx.getResourceManager().getResourceFile(vert),
                _ctx.getResourceManager().getResourceFile(frag), defs);
            if (pstate == null) {
                return false;
            }
            _programIds.put(key, programId = pstate.getProgramID());
        }
        sstate.setProgramID(programId);
        for (ShaderUniform uniform : sstate.uniforms) {
            uniform.uniformID = -1;
        }
        sstate.setNeedsRefresh(true);
        return true;
    }

    /** Identifies a cached shader. */
    protected static class ShaderKey
    {
        /** The name of the vertex shader (or <code>null</code> for none). */
        public String vert;

        /** The name of the fragment shader (or <code>null</code> for none). */
        public String frag;

        /** The set of preprocessor definitions. */
        public HashSet<String> defs = new HashSet<String>();

        public ShaderKey (String vert, String frag, String[] defs)
        {
            this.vert = vert;
            this.frag = frag;
            Collections.addAll(this.defs, defs);
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return (vert == null ? 0 : vert.hashCode()) + (frag == null ? 0 : frag.hashCode()) +
                defs.hashCode();
        }

        @Override // documentation inherited
        public boolean equals (Object obj)
        {
            ShaderKey okey = (ShaderKey)obj;
            return ObjectUtil.equals(vert, okey.vert) && ObjectUtil.equals(frag, okey.frag) &&
                defs.equals(okey.defs);
        }

        @Override // documentation inherited
        public String toString ()
        {
            return "[vert=" + vert + ", frag=" + frag + ", defs=" + defs + "]";
        }
    }

    protected BasicContext _ctx;

    /** Maps shader keys to linked program ids. */
    protected HashMap<ShaderKey, Integer> _programIds = new HashMap<ShaderKey, Integer>();
}
