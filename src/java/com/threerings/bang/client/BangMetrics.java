//
// $Id$

package com.threerings.bang.client;

import java.awt.Color;

import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;

/**
 * Defines sizes of things.
 */
public class BangMetrics
{
    /** The length of the edge of one board square. */
    public static final int SQUARE = 40;

    /** The dimensions of a single board tile. */
    public static final float TILE_SIZE = 10;

    /** Our definition of the left vector. */
    public static final Vector3f LEFT = new Vector3f(1, 0, 0);

    /** Our definition of the up vector. */
    public static final Vector3f UP = new Vector3f(0, 0, 1);

    /** The direction our models are facing (this has nothing to do with
     * the coordinate system or the up and left vectors). */
    public static final Vector3f FORWARD = new Vector3f(0, -1, 0);

    /** Java colors for each of the players. */
    public static final Color[] PIECE_COLORS = {
        Color.blue.brighter(), Color.red, Color.green, Color.yellow
    };

    /** JME colors for each of the players. */
    public static final ColorRGBA[] JPIECE_COLORS = {
        new ColorRGBA(0, 0.5f, 1, 1),
        ColorRGBA.red,
        ColorRGBA.green,
        new ColorRGBA(1, 1, 0, 1)
    };

    /** Darker JME colors for each of the players. */
    public static final ColorRGBA[] DARKER_COLORS = {
        JPIECE_COLORS[0].mult(ColorRGBA.darkGray),
        JPIECE_COLORS[1].mult(ColorRGBA.darkGray),
        JPIECE_COLORS[2].mult(ColorRGBA.darkGray),
        JPIECE_COLORS[3].mult(ColorRGBA.darkGray)
    };
    
    /** Colorization ids for each of the players. */
    public static final int[] PIECE_COLOR_IDS = { 1, 2, 3, 4 };
}
