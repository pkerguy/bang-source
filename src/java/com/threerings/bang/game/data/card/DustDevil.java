//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ResurrectEffect;

/**
 * A card that allows the player to immediately resurrect one dead unit,
 * potentially stealing it from their opponent in the process.
 */
public class DustDevil extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "dust_devil";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return 0;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 10;
    }

    @Override // documentation inherited
    public Effect activate (int x, int y)
    {
        return new ResurrectEffect(owner, x, y);
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 0;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 0;
    }
}
