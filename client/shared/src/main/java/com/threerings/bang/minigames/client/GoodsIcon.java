//
// $Id$

package com.threerings.bang.minigames.client;

import com.jmex.bui.icon.BIcon;
import com.jmex.bui.util.Insets;
import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.minigames.data.MinigameGood;
import com.threerings.bang.store.data.CardTripletGood;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.util.BasicContext;
import com.threerings.presents.dobj.DObject;
import com.threerings.util.MessageBundle;

/**
 * Displays a salable good.
 */
public class GoodsIcon extends PaletteIcon
{
    /** Contains our randomly selected color ids for colorized goods. */
    public int[] colorIds = new int[3];

    public GoodsIcon (BasicContext ctx, DObject entity, MinigameGood good)
    {
        _ctx = ctx;
        _entity = entity;
        setGood(good);
    }

    public MinigameGood getGood ()
    {
        return _good;
    }

    public void setGood (MinigameGood good)
    {
        _good = good;
        setIcon(_good.createIcon(_ctx, _entity, colorIds));
        setText(_ctx.xlate(BangCodes.MINIGAME_MSGS, good.getName()));
        String msg = MessageBundle.compose(
            "m.goods_icon", good.getName(), good.getToolTip());
        setTooltipText(_ctx.xlate(BangCodes.GOODS_MSGS, msg));
    }

    @Override // documentation inherited
    public Insets getInsets ()
    {
        // adjust the insets to accomodate irregularly sized icons
        Insets insets = super.getInsets();
        BIcon icon = getIcon();
        if (icon != null) {
            insets = new Insets(
                insets.left, insets.top + (EXPECTED_ICON_HEIGHT - icon.getHeight())/2,
                insets.right, insets.bottom);
        }
        return insets;
    }

    protected BasicContext _ctx;
    protected DObject _entity;
    protected MinigameGood _good;

    protected static final int EXPECTED_ICON_HEIGHT = 128;
}
