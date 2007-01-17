//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;

/**
 * Represents a purchased full-length music track.
 */
public class Song extends Item
{
    /** Creates a song "pass" for the specified song and player. */
    public Song (int ownerId, String song)
    {
        super(ownerId);
        _song = song;
    }

    /** A default constructor used for serialization. */
    public Song ()
    {
    }

    /**
     * Returns the identifier of the song for which this item grants access.
     */
    public String getSong ()
    {
        return _song;
    }

    @Override // from Item
    public String getName ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.song_" + _song);
    }

    @Override // from Item
    public String getTooltip ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.song_tip");
    }

    @Override // from Item
    public String getIconPath ()
    {
        return "goods/song.png";
    }

    @Override // from Item
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", song=").append(_song);
    }

    protected String _song;
}