//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.IntegerDocument;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ResultListener;

import com.threerings.util.BrowserUtil;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.RequestDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangAuthCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;

/**
 * Displays information about the gang for its members: notoriety, statement, contents of coffers,
 * etc.
 */
public class GangInfoView extends BContainer
    implements ActionListener, AttributeChangeListener
{
    public GangInfoView (
        BangContext ctx, HideoutObject hideoutobj, GangObject gangobj, StatusLabel status)
    {
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(HideoutCodes.HIDEOUT_MSGS);
        _hideoutobj = hideoutobj;
        _gangobj = gangobj;
        _status = status;
        
        GroupLayout glay = GroupLayout.makeVert(GroupLayout.TOP);
        glay.setOffAxisJustification(GroupLayout.RIGHT);
        setLayoutManager(glay);
        
        add(new BLabel(new ImageIcon(_ctx.loadImage("ui/hideout/design_top.png")),
            "gang_info_design"));
            
        glay = GroupLayout.makeHoriz(GroupLayout.CENTER);
        glay.setOffAxisJustification(GroupLayout.BOTTOM);
        BContainer mcont = new BContainer(glay);
        add(mcont);
        
        BContainer tcont = new BContainer(GroupLayout.makeVert(
            GroupLayout.STRETCH, GroupLayout.TOP, GroupLayout.NONE));
        tcont.setStyleClass("gang_info_content");
        mcont.add(tcont);
        tcont.add(new BLabel(gangobj.name.toString().toUpperCase(), "gang_title"),
            GroupLayout.FIXED);
        
        BContainer ncont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ncont.add(_ranking = new BLabel("\"OUTLAWS\"", "gang_notoriety"));
        ncont.add(new BLabel(new ImageIcon(_ctx.loadImage("ui/hideout/diamond.png"))));
        ncont.add(_notoriety = new BLabel(getNotorietyText(), "gang_notoriety"));
        tcont.add(ncont, GroupLayout.FIXED);
        
        BContainer scont = GroupLayout.makeVBox(GroupLayout.CENTER);
        scont.add(_statement = new BLabel(gangobj.statement, "gang_statement"));
        BContainer pcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        pcont.add(_page = new BButton(_msgs.get("m.home_page"), this, "page"));
        _page.setStyleClass("alt_button");
        _page.setVisible(gangobj.getURL() != null);
        if (_ctx.getUserObject().gangRank == GangCodes.LEADER_RANK) {
            pcont.add(_edit = new BButton(_msgs.get("m.edit"), this, "edit"));
            _edit.setStyleClass("alt_button");
        }
        scont.add(pcont);
        tcont.add(scont);
        
        BContainer ccont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ccont.add(new BLabel(_msgs.get("m.coffers"), "coffer_label"));
        _coffers = new MoneyLabel(_ctx);
        _coffers.setStyleClass("gang_coffers");
        _coffers.setMoney(_gangobj.scrip, _gangobj.coins, false);
        ccont.add(_coffers);
        if (_ctx.getUserObject().canDonate()) {
            ccont.add(_donate = new BButton(_msgs.get("m.donate"), this, "donate"));
            _donate.setStyleClass("alt_button");
        }
        tcont.add(ccont, GroupLayout.FIXED);
        
        add(new BLabel(new ImageIcon(_ctx.loadImage("ui/hideout/design_bottom.png")),
            "gang_info_design"));
    }
    
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("page")) {
            BrowserUtil.browseURL(_gangobj.getURL(), new ResultListener() {
                public void requestCompleted (Object result) {
                }
                public void requestFailed (Exception cause) {
                    String msg = MessageBundle.tcompose("m.browser_launch_failed", _gangobj.url);
                    _status.setStatus(BangAuthCodes.AUTH_MSGS, msg, true);
                }
            });
        } if (action.equals("edit")) {
            _ctx.getBangClient().displayPopup(new StatementDialog(_ctx, _status), true, 400);
        } else if (action.equals("donate")) {
            _ctx.getBangClient().displayPopup(new DonateDialog(_ctx, _status), true, 400);
        }
    }
    
    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        String name = event.getName();
        if (name.equals(GangObject.STATEMENT)) {
            _statement.setText(_gangobj.statement);
        } else if (name.equals(GangObject.URL)) {
            _page.setVisible(_gangobj.getURL() != null);
        } else if (name.equals(GangObject.NOTORIETY)) {
            _notoriety.setText(getNotorietyText());
        } else if (name.equals(GangObject.SCRIP) || name.equals(GangObject.COINS)) {
            _coffers.setMoney(_gangobj.scrip, _gangobj.coins, true);
        }
    }
    
    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _gangobj.addListener(this);
    }
    
    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _gangobj.removeListener(this);
    }
    
    protected String getNotorietyText ()
    {
        return _msgs.get("m.notoriety", Integer.toString(_gangobj.notoriety));
    }
    
    protected class StatementDialog extends RequestDialog
    {
        public StatementDialog (BangContext ctx, StatusLabel status)
        {
            super(ctx, HideoutCodes.HIDEOUT_MSGS, "m.statement_tip", "m.update", "m.cancel",
                "m.statement_updated", status);
            
            BContainer scont = GroupLayout.makeHBox(GroupLayout.CENTER);
            scont.add(new BLabel(_msgs.get("m.statement")), GroupLayout.FIXED);
            scont.add(_statement = new BTextField(
                _gangobj.statement, HideoutCodes.MAX_STATEMENT_LENGTH));
            _statement.setPreferredWidth(300);
            add(1, scont);
            
            BContainer ucont = GroupLayout.makeHBox(GroupLayout.CENTER);
            ucont.add(new BLabel(_msgs.get("m.url")), GroupLayout.FIXED);
            ucont.add(_url = new BTextField(_gangobj.url, HideoutCodes.MAX_URL_LENGTH));
            _url.setPreferredWidth(300);
            add(2, ucont);
        }
        
        // documentation inherited
        protected void fireRequest (Object result)
        {
            _hideoutobj.service.setStatement(
                _ctx.getClient(), _statement.getText(), _url.getText(), this);
        }
        
        protected BTextField _statement, _url;
    }
    
    protected class DonateDialog extends RequestDialog
        implements TextListener
    {
        public DonateDialog (BangContext ctx, StatusLabel status)
        {
            super(ctx, HideoutCodes.HIDEOUT_MSGS, "m.donate_tip", "m.donate", "m.cancel",
                "m.donated", status);
            
            // add the amount entry panel
            BContainer acont = GroupLayout.makeHBox(GroupLayout.CENTER);
            add(1, acont);
            
            acont.add(new BLabel(BangUI.scripIcon));
            acont.add(_scrip = new BTextField(4));
            _scrip.setPreferredWidth(50);
            _scrip.setDocument(new IntegerDocument(true));
            _scrip.addListener(this);
            acont.add(new BLabel(_msgs.get("m.and")));
            acont.add(new BLabel(BangUI.coinIcon));
            acont.add(_coins = new BTextField(4));
            _coins.setPreferredWidth(50);
            _coins.setDocument(new IntegerDocument(true));
            _coins.addListener(this);
            
            _buttons[0].setEnabled(false);
        }

        // documentation inherited from interface TextListener
        public void textChanged (TextEvent event)
        {
            try {
                _buttons[0].setEnabled(
                    parseInt(_scrip.getText()) > 0 ||
                    parseInt(_coins.getText()) > 0);
            } catch (NumberFormatException e) {
                _buttons[0].setEnabled(false);
            }
        }
        
        // documentation inherited
        protected void fireRequest (Object result)
        {
            _hideoutobj.service.addToCoffers(_ctx.getClient(),
                parseInt(_scrip.getText()), parseInt(_coins.getText()), this);
        }

        /**
         * Parses the specified string as an integer, allowing the empty
         * string to represent zero.
         */
        protected int parseInt (String text)
        {
            return (text.length() == 0) ? 0 : Integer.parseInt(text);
        }
    
        protected BTextField _scrip, _coins;
    }
    
    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    
    protected BLabel _ranking, _notoriety, _statement;   
    protected MoneyLabel _coffers;
    protected BButton _page, _edit, _donate;
    
    protected StatusLabel _status;
}
