//
// $Id$

package com.threerings.bang.server.persist;

import com.samskivert.util.StringUtil;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;

import java.sql.Timestamp;

/**
 * A record containing persistent information maintained about a Bang!
 * player.
 */
public class IPRecord
{

    /** This player's unique IP */
    public String ip;

    /** The authentication account name associated with this player. */
    public String username;

    /** Check if player IP check is allowed for more than 1. */
    public boolean override;

    /** A blank constructor used when loading records from the database. */
    public IPRecord()
    {
    }

    /** Constructs a blank player record for the supplied account. */
    public IPRecord(String accountName, String address)
    {
        this.ip = address;
        this.username = accountName;
    }
}
