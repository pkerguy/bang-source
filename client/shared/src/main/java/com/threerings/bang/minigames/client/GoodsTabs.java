//
// $Id$

package com.threerings.bang.minigames.client;

import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.util.BangContext;

/**
 * Displays the tabs next to the {@link GoodsPalette} and configures it with
 * the proper filters when selected.
 */
public class GoodsTabs extends HackyTabs
{
    public GoodsTabs (BangContext ctx, GoodsPalette palette)
    {
        super(ctx, true, "ui/store/tab_", TABS, 76, 30);
        _palette = palette;
    }

    @Override // documentation inherited
    protected void tabSelected (int index)
    {
        _palette.setFilter(_filters[index]);
    }

    protected abstract class ArticleGoodFilter implements GoodsPalette.Filter
    {
        public boolean isValid (Good good) {
            if (good instanceof ArticleGood) {
                ArticleCatalog.Article article =
                    _ctx.getAvatarLogic().getArticleCatalog().getArticle(
                        good.getType());
                return (article != null) && isValid(article);
            }
            return false;
        }
        public abstract boolean isValid (ArticleCatalog.Article article);
    }

    protected GoodsPalette.Filter[] _filters = new GoodsPalette.Filter[] {
    };

    protected GoodsPalette _palette;

    protected static final String[] TABS = {
        "items" };
}
