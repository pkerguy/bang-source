//
// $Id$

package com.threerings.bang.avatar.client;

import com.jmex.bui.*;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.layout.BGroup;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.LengthLimitedDocument;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.Runnables;
import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.client.BangClient;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.NameFactory;
import com.threerings.util.MessageBundle;

import java.util.HashSet;

/**
 * Displays an interface via which the player can create their avatar: name,
 * sex and default look.
 */
public class FixAvatarView extends SteelWindow
    implements BangClient.NonClearablePopup
{
    /**
     * Shows the create character interface.
     */
    public static void show (BangContext ctx)
    {
        show(ctx, Runnables.NOOP);
    }

    /**
     * Shows the create character interface and calls the supplied runnable if and when the player
     * creates their character.
     */
    public static void show (BangContext ctx, Runnable onCreate)
    {
        FixAvatarView view = new FixAvatarView(ctx);
        view._onCreate = onCreate;
        ctx.getBangClient().displayPopup(view, true, WIDTH_HINT);
    }

    protected FixAvatarView(BangContext ctx)
    {
        super(ctx, "Fix your avatar");
        _contents.setLayoutManager(BGroup.vert().alignCenter().gap(15).make());

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(AvatarCodes.AVATAR_MSGS);
        setModal(true);

        _contents.add(new BLabel(_msgs.get("m.create_intro"), "dialog_text"));
        _contents.setStyleClass("padded");

        BContainer inner = BGroup.vert().offStretch().alignTop().gap(15).makeBox();
        inner.setStyleClass("fa_inner_box");
        _contents.add(inner);
        _status = new StatusLabel(ctx);
        _status.setStyleClass("dialog_text");
        _contents.add(_status);
        _buttons.add(new BButton(_msgs.get("m.cancel"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _ctx.getBangClient().clearPopup(FixAvatarView.this, true);
                _ctx.getBangClient().createAvatarDismissed(false);
            }
        }, "cancel"));
        _buttons.add(_done = new BButton(_msgs.get("m.done"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                fixAvatar();
            }
        }, "done"));
        _done.setEnabled(true);

        // this all goes in the inner box
        BContainer row = BGroup.horizStretch().offStretch().makeBox();
        BContainer col = BGroup.horiz().alignLeft().makeBox();
        col.add(new Spacer(20, 1));
        col.add(new BLabel(_msgs.get("m.persuasion"), "dialog_label"));
        String[] gensel = new String[] {
                _msgs.get("m.male"), _msgs.get("m.female") };
        col.add(_gender = new BComboBox(gensel));
        _gender.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _look.setGender(_gender.getSelectedIndex() == 0);
                maybeClearStatus();
            }
        });
        row.add(col);
        inner.add(row);
        inner.add(_look = new FirstLookView(ctx, _status));
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        // start with a random gender which will trigger name list and avatar
        // display configuration
        _gender.selectItem(RandomUtil.getInt(2));
    }

    protected void fixAvatar ()
    {
        AvatarService asvc = _ctx.getClient().requireService(AvatarService.class);
        AvatarService.ConfirmListener cl = new AvatarService.ConfirmListener() {
            public void requestProcessed () {
                // move to the next phase of the intro
                _ctx.getBangClient().clearPopup(FixAvatarView.this, true);
                _ctx.getBangClient().createAvatarDismissed(true);
                _onCreate.run();
            }
            public void requestFailed (String reason) {
                _status.setStatus(_msgs.xlate(reason), true);
                _failed = true;
                _done.setEnabled(true);
            }
        };
        _done.setEnabled(true);

        Handle handle = _ctx.getUserObject().handle;
        boolean isMale = _ctx.getUserObject().isMale;
        asvc.createAvatar(handle, isMale, _look.getLookConfig(),
                          _look.getDefaultArticleColorizations(), cl);
    }

    protected void maybeClearStatus ()
    {
        if (_failed) {
            _status.setStatus(_msgs.get("m.create_defstatus"), false);
        }
    }



    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected StatusLabel _status;
    protected boolean _failed;
    protected Runnable _onCreate = Runnables.NOOP;

    protected BComboBox _gender;
    protected FirstLookView _look;
    protected BButton _done;

    protected static final int WIDTH_HINT = 800;
}
