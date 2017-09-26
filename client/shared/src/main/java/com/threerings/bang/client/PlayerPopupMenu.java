//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.*;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;

import com.threerings.bang.admin.client.AdminDialog;
import com.threerings.bang.admin.client.GameMasterDialog;
import com.threerings.bang.data.*;
import com.threerings.presents.client.InvocationService;
import com.threerings.util.MessageBundle;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.chat.client.PardnerChatView;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.gang.client.InviteMemberDialog;
import com.threerings.bang.saloon.data.ParlorObject;

import com.threerings.bang.util.BangContext;

/**
 * A popup menu that can (and should) be displayed any time a player right clicks on another
 * player's avatar.
 */
public class PlayerPopupMenu extends BPopupMenu
    implements ActionListener
{
    /**
     * Checks for a mouse click and popups up the specified player's context menu if appropriate.
     * Assumes that since we're looking the player up by oid, we're in the same room as them.
     */
    public static boolean checkPopup (
        BangContext ctx, BWindow parent, BEvent event, int playerOid)
    {
        // avoid needless occupant info lookups
        if (!(event instanceof MouseEvent)) {
            return false;
        }
        BangOccupantInfo boi = (BangOccupantInfo)
            ctx.getOccupantDirector().getOccupantInfo(playerOid);
        return (boi == null) ? false : checkPopup(ctx, parent, event, (Handle)boi.username, true);
    }

    /**
     * Checks for a mouse click and popups up the specified player's context menu if appropriate.
     *
     * @param isPresent indicates whether or not the other player is present (chatting with us)
     * versus on a high score list or something, for whom it would not make sense to provide
     * options to mute or complain about the player.
     */
    public static boolean checkPopup (BangContext ctx, BWindow parent, BEvent event, Handle handle,
                                      boolean isPresent)
    {
        if (event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            if (mev.getType() == MouseEvent.MOUSE_PRESSED) {
                PlayerPopupMenu menu = new PlayerPopupMenu(ctx, parent, handle, isPresent);
                menu.popup(mev.getX(), mev.getY(), false);
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a popup menu for the specified player.
     */
    public PlayerPopupMenu (BangContext ctx, BWindow parent, Handle handle, boolean isPresent)
    {
        super(parent);

        _ctx = ctx;
        _handle = handle;
        addListener(this);
        setLayer(BangUI.POPUP_MENU_LAYER);

        // add their name as a non-menu item
        add(createTitle());

        // add our menu items
        addMenuItems(isPresent);
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        switch (event.getAction()) {
            case "mute":
                _ctx.getMuteDirector().setMuted(_handle, true);

                break;
            case "unmute":
                _ctx.getMuteDirector().setMuted(_handle, false);

                break;
            case "boot":
                PlaceObject pobj = _ctx.getLocationDirector().getPlaceObject();
                if (pobj instanceof ParlorObject) {
                    ParlorObject parlor = (ParlorObject) pobj;
                    BangOccupantInfo boi = (BangOccupantInfo) parlor.getOccupantInfo(_handle);
                    if (boi != null) {
                        parlor.service.bootPlayer(boi.getBodyOid());
                    }
                }

                break;
            case "chat_pardner":
                PardnerChatView pchat = _ctx.getBangClient().getPardnerChatView();
                if (pchat.display(_handle, true) && !pchat.isAdded()) {
                    _ctx.getBangClient().clearPopup(_parentWindow, true);
                }

                break;
            case "watch_pardner":
                PardnerEntry entry = _ctx.getUserObject().pardners.get(_handle);
                if (entry != null && entry.gameOid > 0) {
                    _ctx.getLocationDirector().moveTo(entry.gameOid);
                }

                break;
            case "remove_pardner": {
                String msg = MessageBundle.tcompose("m.confirm_remove", _handle);
                OptionDialog.showConfirmDialog(
                        _ctx, BangCodes.BANG_MSGS, msg, new OptionDialog.ResponseReceiver() {
                            public void resultPosted(int button, Object result) {
                                if (button == OptionDialog.OK_BUTTON) {
                                    removePardner();
                                }
                            }
                        });

                break;
            }
            case "invite_pardner":
                _ctx.getBangClient().displayPopup(
                new InvitePardnerDialog(_ctx, null, _handle), true, 400, true);

                break;
            case "invite_member":
                _ctx.getBangClient().displayPopup(
                        new InviteMemberDialog(_ctx, null, _handle), true, 400, true);

                break;
            case "view_poster":
                WantedPosterView.displayWantedPoster(_ctx, _handle);

                break;
            case "support_warn":
                _ctx.getBangClient().displayPopup(new GameMasterDialog(_ctx, _handle, "Warn Player", GameMasterDialog.WARN), true, 500);
                break;
            case "support_boot":
                _ctx.getBangClient().displayPopup(new GameMasterDialog(_ctx, _handle, "Boot Player", GameMasterDialog.KICK), true, 500);
                break;
            case "support_tempban":
                _ctx.getBangClient().displayPopup(new GameMasterDialog(_ctx, _handle, "Tempban Player", GameMasterDialog.TEMP_BAN), true, 500);
                break;
            case "support_ban":
                _ctx.getBangClient().displayPopup(new GameMasterDialog(_ctx, _handle, "Permban Player", GameMasterDialog.PERMA_BAN), true, 500);
                break;
            case "admin_scrip":
                _ctx.getBangClient().displayPopup(new AdminDialog(_ctx, _handle, "Grant Scrip", "Scrip:", AdminDialog.GRANT_SCRIP), true, 500);
                break;
            case "admin_removescrip":
                _ctx.getBangClient().displayPopup(new AdminDialog(_ctx, _handle, "Remove Scrip", "Scrip:", AdminDialog.REMOVE_SCRIP), true, 500);
                break;
            case "admin_resetscrip":
                OptionDialog.showConfirmDialog(
                        _ctx, null, "This will set their scrip to 0!", new String[]{"m.ok", "m.cancel"}, (button, result) -> {
                            if (button == 0) {
                                _ctx.getClient().requireService(PlayerService.class).adminAction(
                                        _handle, AdminDialog.RESET_SCRIP, "",
                                        new InvocationService.ConfirmListener() {
                                            @Override
                                            public void requestProcessed() {
                                                OptionDialog.showConfirmDialog(
                                                        _ctx, null, "Success!", new String[]{"m.ok"}, new OptionDialog.ResponseReceiver() {
                                                            public void resultPosted(int button, Object result) {
                                                                // Automatically dismisses the dialog so nothing needed to be done here
                                                            }
                                                        });
                                            }

                                            @Override
                                            public void requestFailed(String cause) {
                                                OptionDialog.showConfirmDialog(
                                                        _ctx, null, "Failed!", new String[]{"m.ok"}, new OptionDialog.ResponseReceiver() {
                                                            public void resultPosted(int button, Object result) {
                                                                // Automatically dismisses the dialog so nothing needed to be done here
                                                            }
                                                        });
                                            }
                                        });
                            }
                        });
                break;
            case "admin_grantbadge":
                _ctx.getBangClient().displayPopup(new AdminDialog(_ctx, _handle, "Grant Badge", "Badge:", AdminDialog.GRANT_BADGE), true, 500);
                break;
            case "admin_removebadge":
                _ctx.getBangClient().displayPopup(new AdminDialog(_ctx, _handle, "Remove Badge", "Badge:", AdminDialog.REMOVE_BADGE), true, 500);
                break;
            case "admin_resetbadge":

                OptionDialog.showConfirmDialog(
                        _ctx, null, "This will remove all of their badges!", new String[]{"m.ok", "m.cancel"}, (button, result) -> {
                            if (button == 0) {
                                _ctx.getClient().requireService(PlayerService.class).adminAction(
                                        _handle, AdminDialog.RESET_BADGE, "",
                                        new InvocationService.ConfirmListener() {
                                            @Override
                                            public void requestProcessed() {
                                                OptionDialog.showConfirmDialog(
                                                        _ctx, null, "Success!", new String[]{"m.ok"}, new OptionDialog.ResponseReceiver() {
                                                            public void resultPosted(int button, Object result) {
                                                                // Automatically dismisses the dialog so nothing needed to be done here
                                                            }
                                                        });
                                            }

                                            @Override
                                            public void requestFailed(String cause) {
                                                OptionDialog.showConfirmDialog(
                                                        _ctx, null, "Failed!", new String[]{"m.ok"}, new OptionDialog.ResponseReceiver() {
                                                            public void resultPosted(int button, Object result) {
                                                                // Automatically dismisses the dialog so nothing needed to be done here
                                                            }
                                                        });
                                            }
                                        });
                            }
                        });
                break;
        }
    }

    protected BComponent createTitle ()
    {
        String title = "@=u(" + _handle.toString() + ")";
        return new BLabel(title, "popupmenu_title");
    }

    protected void addMenuItems (boolean isPresent)
    {
        MessageBundle msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        PlayerObject self = _ctx.getUserObject();
        // if we're in a parlor, we may be able to boot the player
        PlaceObject pobj = _ctx.getLocationDirector().getPlaceObject();
        boolean isParlor = (pobj instanceof ParlorObject);
        boolean isGame = (pobj instanceof BangObject);

        // add an item for viewing their wanted poster
        addMenuItem(new BMenuItem(msgs.get("m.pm_view_poster"), "view_poster"));

        // stop here if this is us or we're anonymous
        if (self.tokens.isAnonymous() || self.handle.equals(_handle)){
            return;
        }

        // add an item for viewing their wanted poster
        if (isPresent) {
            // if we're in a parlor, we may be able to boot the player
            if (isParlor) {
                ParlorObject parlor = (ParlorObject)pobj;
                if (parlor.info.powerUser(self) && parlor.getOccupantInfo(_handle) != null) {
                    addMenuItem(new BMenuItem(msgs.get("m.pm_boot"), "boot"));
                }
            }
        }

        // if they're our pardner, add some pardner-specific items
        PardnerEntry entry = self.pardners.get(_handle);
        if (entry != null) {
            if (!isGame && entry.isAvailable()) {
                addMenuItem(new BMenuItem(msgs.get("m.pm_chat_pardner"), "chat_pardner"));
            }
            if (!isGame && entry.gameOid > 0) {
                addMenuItem(new BMenuItem(msgs.get("m.pm_watch_pardner"), "watch_pardner"));
            }
            addMenuItem(new BMenuItem(msgs.get("m.pm_remove_pardner"), "remove_pardner"));

        } else if (isPresent) {
            // otherwise add an item for inviting them to be our pardner
            addMenuItem(new BMenuItem(msgs.get("m.pm_invite_pardner"), "invite_pardner"));
        }

        // add gang invitation option if they're either present or a pardner
        if (shouldShowGangInvite() && (isPresent || entry != null)) {
            addMenuItem(new BMenuItem(_ctx.xlate(BangCodes.BANG_MSGS, "m.pm_invite_member"),
                "invite_member"));
        }

        // add an item for muting/unmuting (always allow unmuting, only allow muting if the caller
        // indicates that we're in a context where it is appropriate)
        boolean muted = _ctx.getMuteDirector().isMuted(_handle);
        if (muted || isPresent) {
            String mute = muted ? "unmute" : "mute";
            addMenuItem(new BMenuItem(msgs.get("m.pm_" + mute), mute));
        }

        if (self.tokens.isSupport()) {
            addMenuItem(new BMenuItem("Warn Player", "support_warn"));
            addMenuItem(new BMenuItem("Kick Player", "support_boot"));
            addMenuItem(new BMenuItem("Temporary Ban Player", "support_tempban"));
            addMenuItem(new BMenuItem("Ban Player", "support_ban"));
        }
        if (self.tokens.isAdmin()) {
            addMenuItem(new BMenuItem("Grant Scrip", "admin_scrip"));
            addMenuItem(new BMenuItem("Remove Scrip", "admin_removescrip"));
            addMenuItem(new BMenuItem("RESET Scrip", "admin_resetscrip"));
            addMenuItem(new BMenuItem("Grant Badge", "admin_grantbadge"));
            addMenuItem(new BMenuItem("Remove Badge", "admin_removebadge"));
            addMenuItem(new BMenuItem("RESET Badge", "admin_resetbadge"));

        }
    }

    /**
     * Checks whether we should show the "invite into gang" option.
     */
    protected boolean shouldShowGangInvite ()
    {
        return _ctx.getUserObject().canRecruit();
    }

    protected void bootPlayer ()
    {
        PlayerService psvc = _ctx.getClient().requireService(PlayerService.class);
        psvc.bootPlayer(_handle, new PlayerService.ConfirmListener() {
            public void requestProcessed () {
                String msg = MessageBundle.tcompose("m.player_booted", _handle);
                _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, msg);
            }
            public void requestFailed (String cause) {
                _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, cause);
            }
        });
    }

    protected void removePardner ()
    {
        PlayerService psvc = _ctx.getClient().requireService(PlayerService.class);
        psvc.removePardner(_handle, new PlayerService.ConfirmListener() {
            public void requestProcessed () {
                String msg = MessageBundle.tcompose("m.pardner_removed", _handle);
                _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, msg);
            }
            public void requestFailed (String cause) {
                _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, cause);
            }
        });
    }

    protected BangContext _ctx;
    protected Handle _handle;

    protected static final int MAX_SUBJECT_LENGTH = 200;
}
