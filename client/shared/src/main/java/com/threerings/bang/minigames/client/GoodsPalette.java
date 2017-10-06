//
// $Id$

package com.threerings.bang.minigames.client;

import com.samskivert.util.ListUtil;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.minigames.data.MinigameGood;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.GoodsObject;
import com.threerings.bang.util.BangContext;
import com.threerings.presents.dobj.DObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Displays a palette of purchasable goods.
 */
public class GoodsPalette extends IconPalette
{
    /** Used to filter the display of goods. */
    public interface Filter
    {
        public boolean isValid(Good good);
    }

    public GoodsPalette (BangContext ctx, int columns, int rows)
    {
        super(null, columns, rows, GoodsIcon.ICON_SIZE, 1);
        _ctx = ctx;
        // setAllowsEmptySelection(false);
    }

    public void init (GoodsObject goodsobj)
    {
    }

    public void setFilter (Filter filter)
    {
    }

    public void reinitGoods (boolean reselectPrevious)
    {
        // reuse existing icons when they require colorizations (both to avoid the expense of
        // recoloring again and to prevent a disconcerting change of all colors on screen)
        HashMap<MinigameGood, GoodsIcon> oicons = new HashMap<MinigameGood, GoodsIcon>();
        for (SelectableIcon icon : _icons) {
            GoodsIcon gicon = (GoodsIcon)icon;
            if (gicon.getGood().getColorizationClasses(_ctx) != null) {
                oicons.put(gicon.getGood(), gicon);
            }
        }

        MinigameGood sgood = (getSelectedIcon() == null) ? null : ((GoodsIcon)getSelectedIcon()).getGood();
        int opage = _page;
        clear();

        // filter out all matching goods
        ArrayList<MinigameGood> filtered = new ArrayList<MinigameGood>();
//        for (MinigameGood good : _goodsobj.getGoods()) {
//            if ((_filter == null || _filter.isValid(good)) && isAvailable(good)) {
//                filtered.add(good);
//            }
//        }

        // now sort and display them
        MinigameGood[] goods = filtered.toArray(new MinigameGood[filtered.size()]);
        Arrays.sort(goods, MinigameGood.BY_SCRIP_AWARD);
        for (int ii = 0; ii < goods.length; ii++) {
            GoodsIcon icon = oicons.get(goods[ii]);
            if (icon == null) {
                icon = new GoodsIcon(_ctx, getColorEntity(), goods[ii]);
            }
            addIcon(icon);
        }

        // reselect the previously selected good if specified and it's still
        // there; otherwise, flip to the previous page (if it still exists)
        if (!isAdded() || _icons.isEmpty()) {
            return;
        }

        if (reselectPrevious) {
            if (sgood != null) {
                int sidx = ListUtil.indexOf(goods, sgood);
                if (sidx != -1) {
                    displayPage(sidx / (_rows * _cols), false, false);
                    _icons.get(sidx).setSelected(true);
                    return;

                } else if (opage * (_rows * _cols) < goods.length) {
                    displayPage(opage, false, false);
                }
            }
            // if we're trying to reselect our previous good but couldn't find it, don't select
            // anything and leave the old good shown in the inspector

        } else if (autoSelectFirstItem()) {
            // select the first thing on the current page
            _icons.get(_page * _rows * _cols).setSelected(true);
        }
    }

    public boolean autoSelectFirstItem ()
    {
        return true;
    }

    /**
     * Determines whether the specified good is available for play
     */
    protected boolean isAvailable (MinigameGood good)
    {
        // TODO: ADD A CHECK HERE FOR MINIGAME AVAILABLITY
        return true;
    }

    /**
     * Returns the entity to use in determining which colors are available.
     */
    protected DObject getColorEntity ()
    {
        return _ctx.getUserObject();
    }

    protected BangContext _ctx;
}
