//
// $Id$

package com.threerings.bang.bounty.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bounty.data.BountyConfig;
import com.threerings.bang.bounty.data.OfficeCodes;
import com.threerings.bang.bounty.data.OfficeObject;

/**
 * Displays the details on a particular bounty and which of the games the user has completed.
 */
public class BountyDetailView extends BContainer
    implements IconPalette.Inspector, ActionListener
{
    public BountyDetailView (BangContext ctx)
    {
        super(new AbsoluteLayout());
        _ctx = ctx;

        add(_oview = new OutlawView(ctx, 1f), new Point(55, 272));
        add(_reward = new BLabel("", "bounty_detail_reward"), new Point(263, 438));
        _reward.setIcon(new ImageIcon(ctx.loadImage("ui/icons/big_scrip.png")));
        _reward.setIconTextGap(10);
        add(_title = new BLabel("", "bounty_detail_title"), new Point(203, 388));
        add(_descrip = new BLabel("", "bounty_detail_descrip"), new Rectangle(203, 273, 184, 110));

        GroupLayout glay = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.BOTTOM, GroupLayout.STRETCH).setGap(2);
        add(_games = new BContainer(glay), new Rectangle(55, 116, 332, 152));
        add(new BLabel(_ctx.xlate(OfficeCodes.OFFICE_MSGS, "m.recent_completers"), "bounty_recent"),
            new Point(30, 93));
        add(_recent = new RecentCompletersView(ctx), new Rectangle(55, 3, 326, 86));
    }

    /**
     * Configures us with our office distributed object when it becomes available.
     */
    public void setOfficeObject (OfficeObject offobj)
    {
        _offobj = offobj;
    }

    // from interface IconPalette.Inspector
    public void iconUpdated (SelectableIcon icon, boolean selected)
    {
        if (!selected) {
            return;
        }

        _config = ((BountyListEntry)icon).config;
        _oview.setOutlaw(_ctx, _config.getOutlaw(), _config.isCompleted(_ctx.getUserObject()));
        _reward.setText(_config.reward.scrip + (_config.reward.hasExtraReward() ? "+" : ""));
        _title.setText(_config.title);
        _descrip.setText(_config.description);
        _games.removeAll();

        _recent.setCompleters(_offobj == null ? null : _offobj.completers.get(_config.ident));

        PlayerObject user = _ctx.getUserObject();
        GroupLayout glay = GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.LEFT, GroupLayout.CONSTRAIN);
        boolean noMorePlay = false;
        for (BountyConfig.GameInfo game : _config.games) {
            BContainer row = new BContainer(glay);
            String key = _config.getStatKey(game.ident);
            boolean completed = user.stats.containsValue(Stat.Type.BOUNTY_GAMES_COMPLETED, key);
            row.add(new BLabel(completed ? BangUI.completed : BangUI.incomplete),
                    GroupLayout.FIXED);
            row.add(new BLabel(game.name));
            String pmsg = _ctx.xlate(OfficeCodes.OFFICE_MSGS, completed ? "m.replay" : "m.play");
            if (!noMorePlay) {
                BButton play = new BButton(pmsg, this, game.ident);
                if (completed) {
                    play.setStyleClass("alt_button");
                } else {
                    noMorePlay = _config.inOrder;
                }
                play.setEnabled(_config.isAvailable(_ctx.getUserObject()));
                row.add(play, GroupLayout.FIXED);
            }
            _games.add(row);
        }

        // TODO: display recent finishers
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        _games.setEnabled(false);
        PlayerService psvc = (PlayerService)_ctx.getClient().requireService(PlayerService.class);
        psvc.playBountyGame(_ctx.getClient(), _config.ident, event.getAction(),
                            new PlayerService.InvocationListener() {
            public void requestFailed (String cause) {
                _ctx.getChatDirector().displayFeedback(OfficeCodes.OFFICE_MSGS, cause);
                _games.setEnabled(true);
            }
        });
    }

    protected BangContext _ctx;
    protected OfficeObject _offobj;
    protected BountyConfig _config;

    protected OutlawView _oview;
    protected BLabel _reward, _title, _descrip;
    protected BContainer _games;
    protected RecentCompletersView _recent;
}
