//
// $Id$

package com.threerings.bang.client;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import com.samskivert.util.Predicate;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.Article;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

/**
 * Displays some subset of the user's inventory.
 */
public class InventoryPalette extends IconPalette
    implements SetListener
{
    /**
     * Creates an {@link InventoryPalette} with 5 columns, 3 rows, and no {@link Inspector}.
     */
    public InventoryPalette (BangContext ctx, Predicate<Item> itemp, boolean allowItemPopup)
    {
        this(ctx, itemp, null, COLUMNS, 3, allowItemPopup);
    }

    /**
     * Creates an {@link InventoryPalette} without {@link Inspector}.
     */
    public InventoryPalette (BangContext ctx, Predicate<Item> itemp,
                             int columns, int rows)
    {
        this(ctx, itemp, null, columns, rows, false);
    }

    /**
     * Creates an {@link InventoryPalette}.
     */
    public InventoryPalette (BangContext ctx, Predicate<Item> itemp, Inspector inspector,
                             int columns, int rows, boolean allowItemPopup)
    {
        super(inspector, columns, rows, ItemIcon.ICON_SIZE, 0);
        setPaintBackground(true);
        _ctx = ctx;
        _itemp = itemp;
        _allowItemPopup = allowItemPopup;
    }

    // documentation inherited from SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (event.getName().equals(PlayerObject.INVENTORY)) {
            Item item = (Item)event.getEntry();
            if (_itemp.isMatch(item)) {
                int idx = 0;
                for (SelectableIcon icon : _icons) {
                    ItemIcon iicon = (ItemIcon)icon;
                    if (_itemComparator.compare(item, iicon.getItem()) < 0) {
                        break;
                    } else {
                        idx++;
                    }
                }
                addIcon(idx, new ItemIcon(_ctx, item));
            }
        }
    }
    
    // documentation inherited from SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        if (event.getName().equals(PlayerObject.INVENTORY)) {
            Item item = (Item)event.getOldEntry();
            if (_itemp.isMatch(item)) {
                removeIcon(getIcon(item));
            }
        }
    }
    
    // documentation inherited from SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (event.getName().equals(PlayerObject.INVENTORY)) {
            Item item = (Item)event.getEntry();
            if (_itemp.isMatch(item)) {
                getIcon(item).setItem(item);
            }
        }
    }
    
    /**
     * Finds the icon corresponding to the specified item.
     */
    protected ItemIcon getIcon (Item item)
    {
        int itemId = item.getItemId();
        for (SelectableIcon icon : _icons) {
            ItemIcon iicon = (ItemIcon)icon;
            if (iicon.getItem().getItemId() == itemId) {
                return iicon;
            }
        }
        return null;
    }
    
    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // populate our item display every time we are shown as we may be
        // hidden, the player's inventory updated, then reshown again
        populate();
        
        // listen to the user object for inventory changes
        _ctx.getUserObject().addListener(this);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // clear out our item display
        clear();
        
        // stop listening to the user object
        _ctx.getUserObject().removeListener(this);
    }

    /**
     * Populates the palette with icons based on the contents of the players
     * inventory.
     */
    protected void populate ()
    {
        PlayerObject user = _ctx.getUserObject();
        Item[] items = user.inventory.toArray(new Item[user.inventory.size()]);
        Arrays.sort(items, _itemComparator);
        // sort the items in some vaguely sensible order
        for (Item item : items) {
            if (!_itemp.isMatch(item)) {
                continue;
            }
            ItemIcon icon = new ItemIcon(_ctx, item);
            if (item instanceof CardItem) {
                icon.setFitted(true);
            }
            icon.setMenuEnabled(_allowItemPopup);
            addIcon(icon);
        }
    }

    protected BangContext _ctx;
    protected Predicate<Item> _itemp;
    protected boolean _allowItemPopup;

    /** Used to sort the inventory display. */
    protected Comparator<Item> _itemComparator = new Comparator<Item>() {
        public int compare (Item one, Item two) {
            if (!one.getClass().equals(two.getClass())) {
                return one.getClass().getName().compareTo(
                    two.getClass().getName());
            }
            // compare articles specially to make Rick happy
            if (one instanceof Article) {
                return Article.ARTICLE_COMP.compare((Article)one, (Article)two);
            } else {
                String t1 = _ctx.xlate(BangCodes.BANG_MSGS, one.getName(false));
                return t1.compareTo(
                    _ctx.xlate(BangCodes.BANG_MSGS, two.getName(false)));
            }
        }
    };
    
    protected static final int COLUMNS = 5;    
}
