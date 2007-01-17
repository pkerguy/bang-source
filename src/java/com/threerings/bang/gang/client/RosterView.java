//
// $Id$

package com.threerings.bang.gang.client;

import java.util.Arrays;

import com.jme.renderer.Renderer;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.samskivert.util.ResultListener;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.client.bui.RequestDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.client.AvatarView;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.data.HideoutCodes;

import static com.threerings.bang.Log.*;

/**
 * Allows the user to browse the list of gang members.
 */
public class RosterView extends BContainer
    implements ActionListener, AttributeChangeListener, SetListener, GangCodes, HideoutCodes
{
    public RosterView (
        BangContext ctx, HideoutObject hideoutobj, GangObject gangobj, BContainer bcont,
        StatusLabel status)
    {
        super(GroupLayout.makeVStretch());
        _ctx = ctx;
        _hideoutobj = hideoutobj;
        _gangobj = gangobj;
        _bcont = bcont;
        _status = status;
        _msgs = ctx.getMessageManager().getBundle(HIDEOUT_MSGS);
        
        setStyleClass("roster_view");
        
        BContainer rcont = new BContainer(GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        ((GroupLayout)rcont.getLayoutManager()).setGap(-7);
            
        BContainer tcont = new BContainer(GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.LEFT, GroupLayout.NONE));
        ((GroupLayout)tcont.getLayoutManager()).setOffAxisJustification(GroupLayout.TOP);
        rcont.add(tcont);
        
        BContainer left = new BContainer(GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        ((GroupLayout)left.getLayoutManager()).setGap(0);
        tcont.add(left);
        
        left.add(new BLabel(_msgs.get("m.leaders"), "roster_title"));
        left.add(new BLabel(new ImageIcon(_ctx.loadImage("ui/hideout/underline_short.png"))));
        left.add(_lcont = new BContainer(new TableLayout(2)));
        _lcont.setStyleClass("roster_table");
        
        tcont.add(_lview = new LeaderView(ctx, status), GroupLayout.FIXED);
        
        BContainer bottom = new BContainer(GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        ((GroupLayout)bottom.getLayoutManager()).setGap(0);
        rcont.add(bottom);
        
        bottom.add(new BLabel(_msgs.get("m.members"), "roster_title"));
        bottom.add(new BLabel(new ImageIcon(_ctx.loadImage("ui/hideout/underline_long.png"))));
        bottom.add(_mcont = new BContainer(new TableLayout(4)));
        _mcont.setStyleClass("roster_table");
        
        BScrollPane rpane = new BScrollPane(rcont);
        rpane.setStyleClass("roster_pane");
        add(rpane);
    }
    
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("history")) {
            _ctx.getBangClient().displayPopup(new HistoryDialog(_ctx, _hideoutobj), true, 500);
        } else if (action.equals("invite")) {
            _ctx.getBangClient().displayPopup(new InviteMemberDialog(_ctx, _status), true, 400);
        } else if (action.equals("leave")) {
            leaveGang();
        }
    }
    
    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (event.getName().equals(GangObject.AVATAR)) {
            _lview.update();
        }
    }
    
    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (!event.getName().equals(GangObject.MEMBERS)) {
            return;
        }
        GangMemberEntry entry = (GangMemberEntry)event.getEntry();
        if (entry.rank == LEADER_RANK) {
            updateLeaders();
        } else {
            updateMembers();
        }
    }
    
    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        if (!event.getName().equals(GangObject.MEMBERS)) {
            return;
        }
        GangMemberEntry entry = (GangMemberEntry)event.getOldEntry();
        if (entry.rank == LEADER_RANK) {
            updateLeaders();
            _lview.update();
        } else {
            updateMembers();
        }
    }
    
    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (!event.getName().equals(GangObject.MEMBERS)) {
            return;
        }
        GangMemberEntry oentry = (GangMemberEntry)event.getOldEntry(),
            nentry = (GangMemberEntry)event.getEntry();
        if (oentry.rank == LEADER_RANK || nentry.rank == LEADER_RANK) {
            updateLeaders();
            _lview.update();
        }
        if (oentry.rank != LEADER_RANK || nentry.rank != LEADER_RANK) {
            updateMembers();
        }
    }
    
    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        updateLeaders();
        updateMembers();
        _lview.update();
        _gangobj.addListener(this);

        // populate the button panel
        _bcont.add(new BButton(_msgs.get("m.history"), this, "history"));
        if (_ctx.getUserObject().canRecruit()) {
            _bcont.add(new BButton(_msgs.get("m.invite"), this, "invite"));
        }
        _bcont.add(new BButton(_msgs.get("m.leave"), this, "leave"));
    }
    
    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _gangobj.removeListener(this);
        _bcont.removeAll();
    }
    
    protected void updateLeaders ()
    {
        _lcont.removeAll();
        for (GangMemberEntry entry : _gangobj.members) {
            if (entry.rank == LEADER_RANK) {
                addMemberEntry(_lcont, entry);
            }
        }
    }
    
    protected void updateMembers ()
    {
        _mcont.removeAll();
        for (GangMemberEntry entry : _gangobj.members) {
            if (entry.rank != LEADER_RANK) {
                addMemberEntry(_mcont, entry);
            }
        }
    }
    
    protected void addMemberEntry (BContainer cont, GangMemberEntry entry)
    {
        cont.add(new MemberLabel(_ctx, entry, false, _status, "roster_entry"));
        cont.add(new BLabel("(" + entry.notoriety + ")", "roster_entry"));
    }
    
    protected void leaveGang ()
    {
        String confirm = MessageBundle.tcompose("m.confirm_leave", _gangobj.name),
            success = MessageBundle.tcompose("m.left", _gangobj.name);
        _ctx.getBangClient().displayPopup(
            new RequestDialog(_ctx, HIDEOUT_MSGS, confirm, "m.ok", "m.cancel", success, _status) {
                protected void fireRequest (Object result) {
                    _hideoutobj.service.leaveGang(_ctx.getClient(), this);
                }        
            }, true, 400);
    }
    
    protected class LeaderView extends MemberLabel
    {
        public LeaderView (BangContext ctx, StatusLabel status)
        {
            super(ctx, false, status, "leader_view");
            setIcon(_aicon = new AvatarIcon(ctx));
            setIconTextGap(0);
            setOrientation(BLabel.VERTICAL);
        }
        
        public void update ()
        {
            setMember(_gangobj.getSeniorLeader());
            _aicon.setAvatar(_gangobj.avatar);
        }
        
        protected AvatarIcon _aicon;
    }
    
    protected class AvatarIcon extends ImageIcon
        implements ResultListener<BImage>
    {
        public AvatarIcon (BangContext ctx)
        {
            super(ctx.loadImage("ui/hideout/leader_frame.png"));
        }
        
        public void setAvatar (int[] avatar)
        {
            if (Arrays.equals(avatar, _avatar)) {
                return;
            }
            AvatarView.getImage(_ctx, avatar, 65, 82, false, this);
            _avatar = avatar;
        }
        
        public void requestCompleted (BImage result)
        {
            if (_aimg != null && _added) {
                _aimg.release();
            }
            _aimg = result;
            if (_added) {
                _aimg.reference();
            }
        }
        
        public void requestFailed (Exception cause)
        {
            log.warning("Failed to retrieve avatar image for leader [cause=" + cause + "].");
        }
        
        public void wasAdded ()
        {
            super.wasAdded();
            if (_aimg != null) {
                _aimg.reference();
            }
            _added = true;
        }
        
        public void wasRemoved ()
        {
            super.wasRemoved();
            if (_aimg != null) {
                _aimg.release();
            }
            _added = false;
        }
        
        public void render (Renderer r, int x, int y, float alpha)
        {
            super.render(r, x, y, alpha);
            if (_aimg != null) {
                _aimg.render(r, x+3, y+3, alpha);
            }
        }
        
        protected int[] _avatar;
        protected BImage _aimg;
        protected boolean _added;
    }
    
    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    protected StatusLabel _status;
    
    protected BContainer _bcont, _lcont, _mcont;
    protected LeaderView _lview;
}