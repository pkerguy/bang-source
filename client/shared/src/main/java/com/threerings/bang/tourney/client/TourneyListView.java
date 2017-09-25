//
// $Id$

package com.threerings.bang.tourney.client;

import com.jmex.bui.*;
import com.jmex.bui.event.*;
import com.jmex.bui.layout.*;
import com.jmex.bui.util.*;
import com.threerings.admin.client.AdminService;
import com.threerings.bang.admin.data.ConfigObject;
import com.threerings.bang.client.bui.TabbedPane;
import com.threerings.bang.data.*;
import com.threerings.bang.tourney.data.*;
import com.threerings.bang.util.*;
import com.threerings.presents.dobj.*;
import com.threerings.presents.dobj.EventListener;
import com.threerings.presents.util.*;
import com.threerings.util.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static com.threerings.bang.Log.*;

/**
 * Displays the runtime configuration objects and allows them to be modified
 * (by admins).
 */
public class TourneyListView extends BDecoratedWindow
        implements ActionListener, EventListener
{
    public TourneyListView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), ctx.xlate("admin", "m.config_title"));
        ((GroupLayout)getLayoutManager()).setOffAxisPolicy(GroupLayout.STRETCH);

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("admin");
        _scrollList = new TourneyListView.TourneyScrollList(new Dimension(450, 300));
        add(_scrollList);
        add(_tabs = new TabbedPane(true));
        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        bcont.add(new BButton("New", this, "new_tourney"));
        bcont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(bcont, GroupLayout.FIXED);
        _safesub = new SafeSubscriber<TourniesObject>(
                ((BangBootstrapData)_ctx.getClient().getBootstrapData()).tourniesOid,
                new Subscriber<TourniesObject>() {
                    public void objectAvailable (TourniesObject tobj) {
                        _tobj = tobj;
                        populateTourneyList();
                    }
                    public void requestFailed (int oid, ObjectAccessException cause) {
                        log.warning("Failed to subscribe to tournies object", "oid", oid,
                                "cause", cause);
                        showError();
                    }
                });

    }

    // documentation inherited from interface AdminService.ConfigInfoListener
    public void requestFailed (String reason)
    {
        log.warning("Failed to get Tournament info", "reason", reason);
    }
    protected void showError ()
    {
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if ("new_tourney".equals(cmd)) {
            _ctx.getBangClient().displayPopup(new TourneyConfigView(_ctx), true, 400);

        } else if ("dismiss".equals(cmd)) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    @Override
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return new Dimension(800, 600);
    }

    // documentation inherited from interface EventListener
    public void eventReceived (DEvent event)
    {
        if (event instanceof NamedEvent) {
            if (TourniesObject.TOURNIES.equals(((NamedEvent)event).getName())) {
                updateList();
            }
        }
    }

    // documentation inherited
    protected void wasAdded ()
    {
        _safesub.subscribe(_ctx.getDObjectManager());
    }

    // documentation inherited
    protected void wasRemoved ()
    {
        _safesub.unsubscribe(_ctx.getDObjectManager());
    }

    /**
     * Called once we've recieved the tournies object.
     */
    protected void populateTourneyList ()
    {
        _tobj.addListener(this);
        updateList();
    }

    /**
     * Called to update the list with the information from the tournies object.
     */
    protected void updateList ()
    {
        _scrollList.removeValues();
        for (TourneyListingEntry entry : _tobj.tournies) {
            _scrollList.addValue(new EntryBuilder(entry, true), false);
        }

    }

    protected class TourneyScrollList extends BScrollingList<TourneyListView.EntryBuilder, BComponent>
    {
        public TourneyScrollList (Dimension size)
        {
            super();

            setPreferredSize(size);
        }

        @Override // from BScrollingList
        protected BComponent createComponent (TourneyListView.EntryBuilder builder)
        {
            return builder.build();
        }
    }

    protected class EntryBuilder {
        public EntryBuilder(TourneyListingEntry entry, boolean cache) {
            _entry = entry;

            if (cache) {
                _cachedEntry = build();
            } else {
                _cachedEntry = null;
            }
        }

        public BComponent build() {
            // if we've got a chache, use it
            if (_cachedEntry != null) {
                return _cachedEntry;
            }

            BContainer cont = GroupLayout.makeHBox(GroupLayout.CENTER);
            cont.add(new BLabel(_entry.desc, "left_label"));
            cont.add(new BButton("Details", TourneyListView.this, "details"));

            return cont;
        }

        protected BComponent _cachedEntry;
        protected TourneyListingEntry _entry;
    }

    protected TourneyListView.TourneyScrollList _scrollList;
    protected SafeSubscriber<TourniesObject> _safesub;
    protected TourniesObject _tobj;
    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected TabbedPane _tabs;
}
