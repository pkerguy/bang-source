//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.station.data.StationCodes;
import com.threerings.bang.util.DeploymentConfig;

/**
 * Represents a train ticket purchased by the player giving them access to a particular town.
 */
public class TrainTicket extends Item
{
    /** A blank constructor used during unserialization. */
    public TrainTicket ()
    {
    }

    /** Creates a new ticket for the specified town. */
    public TrainTicket (int ownerId, int townIndex)
    {
        super(ownerId);
        _townIndex = townIndex;
    }

    /**
     * Returns the index of the town to which this ticket provides access.
     */
    public int getTownIndex ()
    {
        return _townIndex;
    }

    /**
     * Returns the town to which this ticket provides access.
     */
    public String getTownId ()
    {
        return BangCodes.TOWN_IDS[_townIndex];
    }

    /**
     * Returns the cost of this ticket in scrip.
     */
    public int getScripCost ()
    {
        return -1; // Must use coins to get the other town
    }

    /**
     * Returns the cost of this ticket in coins.
     */
    public int getCoinCost (PlayerObject user)
    {
        return 2;
    }

    @Override // documentation inherited
    public String getName ()
    {
        String msg = MessageBundle.qualify(BangCodes.BANG_MSGS, "m." + getTownId());
        msg = MessageBundle.compose(
            getItemId() == 0 ? "m.temporary_ticket" : "m.train_ticket", msg);
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    @Override // documentation inherited
    public String getTooltip (PlayerObject user)
    {
        String msg = MessageBundle.qualify(BangCodes.BANG_MSGS, "m." + getTownId());
        msg = MessageBundle.compose(
            (getItemId() == 0 ? "m.temporary_" : "m.") + "train_ticket_tip", msg);
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "goods/tickets/" + (getItemId() == 0 ? "pass_" : "" ) + getTownId() + ".png";
    }

    @Override // documentation inherited
    public boolean isEquivalent (Item other)
    {
        return super.isEquivalent(other) && ((TrainTicket)other)._townIndex == _townIndex;
    }

    protected int _townIndex;
}
