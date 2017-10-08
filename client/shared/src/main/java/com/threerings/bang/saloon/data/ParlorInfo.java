//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;

/**
 * Contains summary information on a back parlor room.
 */
public class ParlorInfo extends SimpleStreamableObject
    implements DSet.Entry
{
    /** Indicates the type of this parlor. */
    public enum Type { SOCIAL, RECRUITING, NORMAL, PARDNERS_ONLY, PASSWORD };

    /** The player that created the parlor. */
    public Handle creator;

    /** The type of this parlor. */
    public Type type;

    /** The number of occupants in this parlor. */
    public int occupants;

    /** If this parlor has matched games. */
    public boolean matched;

    /** If this is a server parlor. */
    public boolean server;

    /** If this is a tournament's parlor */
    public boolean tournament;

    /** Has this parlor been filled! */
    public boolean filled = false;

    /** Is this a Twitch integerated Parlor? */
    public boolean isTwitch = false;

    /** Stores the Twitch Usernameo of Creator */
    public String twitchUsername;

    /** How many players are allowed entry into this parlor */
    public int maxPlayers; // Should either be 2 or 4.. No more!

    /** The gangId for a recruiting parlor. */
    public int gangId;

    /** Required round id */
    public int roundId;

    // documentation inherited from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return creator;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        ParlorInfo oinfo = (ParlorInfo)other;
        return creator.equals(oinfo.creator) && type == oinfo.type && occupants == oinfo.occupants;
    }

    /**
     * Returns true if this user can control the parlor.
     */
    public boolean powerUser (PlayerObject user)
    {
        if(tournament) return false; // No one can control the tournament parlors.
        // if this player is the creator, or an admin/support, let 'em in regardless
        return user.handle.equals(creator) || user.tokens.isSupport() ||
            (type == Type.RECRUITING && user.gangId == gangId && user.canRecruit());
    }
}
