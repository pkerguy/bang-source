/*
 * Copyright (c) 2003-2006 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jme.scene.state;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme.light.Light;
import com.jme.renderer.ColorRGBA;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.OutputCapsule;
import com.jme.util.geom.BufferUtils;

/**
 * <code>LightState</code> maintains a collection of lights up to the set
 * number of maximum lights allowed. Any subclass of <code>Light</code> can be
 * added to the light state. Each light is processed and used to modify the
 * color of the scene.
 *
 * @author Mark Powell
 * @author Joshua Slack - Light state combining and performance enhancements
 * @author Three Rings: Local model and separate specular
 * @version $Id$
 */
public abstract class LightState extends RenderState {
    /**
     * defines the maximum number of lights that are allowed to be maintained at
     * one time.
     */
    public static final int MAX_LIGHTS_ALLOWED = 8;

    /** Ignore lights. */
    public static final int OFF = 0;

    /**
     * Combine light states starting from the root node and working towards the
     * given SceneElement. Ignore disabled states. Stop combining when lights ==
     * MAX_LIGHTS_ALLOWED
     */
    public static final int COMBINE_FIRST = 1;

    /**
     * Combine light states starting from the given Spatial and working towards
     * the root. Ignore disabled states. Stop combining when lights ==
     * MAX_LIGHTS_ALLOWED
     */
    public static final int COMBINE_CLOSEST = 2;

    /**
     * Similar to COMBINE_CLOSEST, but if a disabled state is encountered, it
     * will stop combining at that point. Stop combining when lights ==
     * MAX_LIGHTS_ALLOWED
     */
    public static final int COMBINE_RECENT_ENABLED = 3;

    /** Inherit mode from parent. */
    public static final int INHERIT = 4;

    /** Do not combine light states, just use the most recent one. */
    public static final int REPLACE = 5;

    /**
     * When applied to lightMask, implies ambient light should be set to 0 for
     * this lightstate
     */
    public static final int MASK_AMBIENT = 1;

    /**
     * When applied to lightMask, implies diffuse light should be set to 0 for
     * this lightstate
     */
    public static final int MASK_DIFFUSE = 2;

    /**
     * When applied to lightMask, implies specular light should be set to 0 for
     * this lightstate
     */
    public static final int MASK_SPECULAR = 4;

    /**
     * When applied to lightMask, implies global ambient light should be set to
     * 0 for this lightstate
     */
    public static final int MASK_GLOBALAMBIENT = 8;

    // holds the lights
    private List<Light> lightList;

    // mask value - default is no masking
    protected int lightMask = 0;

    // mask value stored by pushLightMask, retrieved by popLightMask
    protected int backLightMask = 0;

    /** When true, both sides of the model will be lighted. */
    protected boolean twoSidedOn = false;

    protected float[] globalAmbient = { 0.0f, 0.0f, 0.0f, 1.0f };
    //XXX move to record
    protected static FloatBuffer zeroBuffer;
    protected static FloatBuffer zeroBuffer3;

    /**
     * When true, the eye position (as opposed to just the view direction) will
     * be taken into account when computing specular reflections.
     */
    protected boolean localViewerOn;

    /**
     * When true, specular highlights will be computed separately and added to
     * fragments after texturing.
     */
    protected boolean separateSpecularOn;

    /**
     * Constructor instantiates a new <code>LightState</code> object.
     * Initially there are no lights set.
     */
    public LightState() {
        lightList = new ArrayList<Light>();
        if (zeroBuffer == null) {
            zeroBuffer = BufferUtils.createFloatBuffer(4);
            zeroBuffer.put(0).put(0).put(0).put(1);
            zeroBuffer.rewind();
        }
        if (zeroBuffer3 == null) {
            zeroBuffer3 = BufferUtils.createFloatBuffer(3);
            zeroBuffer3.put(0).put(0).put(0);
            zeroBuffer3.rewind();
        }
    }

    /**
     * <code>getType</code> returns the type of render state this is.
     * (RS_LIGHT).
     *
     * @see com.jme.scene.state.RenderState#getType()
     */
    @Override
    public int getType() {
        return RS_LIGHT;
    }

    /**
     *
     * <code>attach</code> places a light in the queue to be processed. If
     * there are already eight lights placed in the queue, the light is ignored
     * and false is returned. Otherwise, true is returned to indicate success.
     *
     * @param light
     *            the light to add to the queue.
     * @return true if the light was added successfully, false if there are
     *         already eight lights in the queue.
     */
    public boolean attach(Light light) {
        if (lightList.size() < MAX_LIGHTS_ALLOWED) {
            lightList.add(light);
            setNeedsRefresh(true);
            return true;
        }
        return false;
    }

    /**
     *
     * <code>detach</code> removes a light from the queue for processing.
     *
     * @param light
     *            the light to be removed.
     */
    public void detach(Light light) {
        lightList.remove(light);
        setNeedsRefresh(true);
    }

    /**
     *
     * <code>detachAll</code> clears the queue of all lights to be processed.
     *
     */
    public void detachAll() {
        lightList.clear();
        setNeedsRefresh(true);
    }

    /**
     *
     * <code>get</code> retrieves a particular light defined by an index. If
     * there exists no light at a particular index, null is returned.
     *
     * @param i
     *            the index to retrieve the light from the queue.
     * @return the light at the given index, null if no light exists at this
     *         index.
     */
    public Light get(int i) {
        return lightList.get(i);
    }

    /**
     *
     * <code>getQuantity</code> returns the number of lights currently in the
     * queue.
     *
     * @return the number of lights currently in the queue.
     */
    public int getQuantity() {
        return lightList.size();
    }

    /**
     * Sets if two sided lighting should be enabled for this LightState.
     *
     * @param twoSidedOn
     *            If true, two sided lighting is enabled.
     */
    public void setTwoSidedLighting(boolean twoSidedOn) {
        this.twoSidedOn = twoSidedOn;
        setNeedsRefresh(true);
    }

    /**
     * Returns the current state of two sided lighting for this LightState. By
     * default, it is off.
     *
     * @return True if two sided lighting is enabled.
     */
    public boolean getTwoSidedLighting() {
        return this.twoSidedOn;
    }

    /**
     * Sets if local model mode should be enabled for this LightState.
     *
     * @param localViewerOn
     *            If true, local model mode is enabled.
     */
    public void setLocalViewer(boolean localViewerOn) {
        this.localViewerOn = localViewerOn;
        setNeedsRefresh(true);
    }

    /**
     * Returns the current state of local model mode for this LightState. By
     * default, it is off.
     *
     * @return True if local model mode is enabled.
     */
    public boolean getLocalViewer() {
        return this.localViewerOn;
    }

    /**
     * Sets if separate specular mode should be enabled for this LightState.
     *
     * @param separateSpecularOn
     *            If true, separate specular mode is enabled.
     */
    public void setSeparateSpecular(boolean separateSpecularOn) {
        this.separateSpecularOn = separateSpecularOn;
        setNeedsRefresh(true);
    }

    /**
     * Returns the current state of separate specular mode for this LightState.
     * By default, it is off.
     *
     * @return True if separate specular mode is enabled.
     */
    public boolean getSeparateSpecular() {
        return this.separateSpecularOn;
    }

    public void setGlobalAmbient(ColorRGBA color) {
        globalAmbient[0] = color.r;
        globalAmbient[1] = color.g;
        globalAmbient[2] = color.b;
        globalAmbient[3] = color.a;
        setNeedsRefresh(true);
    }

    public ColorRGBA getGlobalAmbient() {
        return new ColorRGBA(globalAmbient[0], globalAmbient[1],
                             globalAmbient[2], globalAmbient[3]);
    }

    /**
     * @return Returns the lightMask - default is 0 or not masked.
     */
    public int getLightMask() {
        return lightMask;
    }

    /**
     * <code>setLightMask</code> sets what attributes of this lightstate to
     * apply as an int comprised of bitwise or'ed values.
     *
     * @param lightMask
     *            The lightMask to set.
     */
    public void setLightMask(int lightMask) {
        this.lightMask = lightMask;
        setNeedsRefresh(true);
    }

    /**
     * Saves the light mask to a back store. That backstore is recalled with
     * popLightMask. Despite the name, this is not a stack and additional pushes
     * will simply overwrite the backstored value.
     */
    public void pushLightMask() {
        backLightMask = lightMask;
    }

    /**
     * Recalls the light mask from a back store or 0 if none was pushed.
     *
     * @see com.jme.scene.state.LightState#pushLightMask()
     */
    public void popLightMask() {
        lightMask = backLightMask;
    }

    @Override
    public void write(JMEExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.writeSavableArrayList(lightList, "lightList", new ArrayList<Light>());
        capsule.write(lightMask, "lightMask", 0);
        capsule.write(backLightMask, "backLightMask", 0);
        capsule.write(twoSidedOn, "twoSidedOn", false);
        capsule.write(globalAmbient, "globalAmbient", new float[]{ 0.0f, 0.0f, 0.0f, 1.0f });
        capsule.write(localViewerOn, "localViewerOn", false);
        capsule.write(separateSpecularOn, "separateSpecularOn", false);
    }

    @Override
    public void read(JMEImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
        lightList = capsule.readSavableArrayList("lightList",new ArrayList<Light>());
        lightMask = capsule.readInt("lightMask", 0);
        backLightMask = capsule.readInt("backLightMask", 0);
        twoSidedOn = capsule.readBoolean("twoSidedOn", false);
        globalAmbient = capsule.readFloatArray("globalAmbient", new float[]{0.0f, 0.0f, 0.0f, 1.0f });
        localViewerOn = capsule.readBoolean("localViewerOn", false);
        separateSpecularOn = capsule.readBoolean("separateSpecularOn", false);
    }

    @Override
    public Class<LightState> getClassTag() {
        return LightState.class;
    }
}
