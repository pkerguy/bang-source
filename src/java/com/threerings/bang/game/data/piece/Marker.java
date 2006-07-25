//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.MarkerSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;

/**
 * A marker piece class used for a variety of purposes.
 */
public class Marker extends Piece
{
    /** A particular marker type. */
    public static final int START = 0;

    /** A particular marker type. */
    public static final int BONUS = 1;

    /** A particular marker type. */
    public static final int CATTLE = 2;

    /** A particular marker type. */
    public static final int LODE = 3;

    /** A particular marker type. */
    public static final int TOTEM = 4;

    /** A particular marker type. */
    public static final int SAFE = 5;

    /**
     * Handy function for checking if this piece is a marker and of the
     * specified type.
     */
    public static boolean isMarker (Piece piece, int type)
    {
        return (piece instanceof Marker) && ((Marker)piece).getType() == type;
    }

    /**
     * Creates a marker of the specified type.
     */
    public Marker (int type)
    {
        _type = type;
    }

    /**
     * Creates a blank marker for use when unserializing.
     */
    public Marker ()
    {
    }

    /**
     * Returns the type of this marker.
     */
    public int getType ()
    {
        return _type;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new MarkerSprite(_type);
    }

    @Override // documentation inherited
    protected int computeOrientation (int nx, int ny)
    {
        // our orientation doesn't change with position
        return orientation;
    }

    /** Indicates the type of this marker. */
    protected int _type;
}
