//
// $Id$

package com.threerings.bang.tourney.client;

import com.jmex.bui.*;
import com.jmex.bui.event.*;
import com.jmex.bui.layout.*;
import com.jmex.bui.util.*;
import com.threerings.bang.data.*;
import com.threerings.bang.tourney.data.*;
import com.threerings.bang.util.*;
import com.threerings.presents.dobj.*;
import com.threerings.presents.dobj.EventListener;
import com.threerings.presents.util.*;
import com.threerings.util.*;

import static com.threerings.bang.Log.*;

/**
 * Shows all currently available tournies.
 */
public class TourneyListView extends BDecoratedWindow
    implements ActionListener, EventListener
{
    public TourneyListView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), "Tournament Administration");

        setModal(true);

        _ctx = ctx;
        _scrollList = new TourneyScrollList(new Dimension(450, 300));
        add(_scrollList);

        BContainer cont = GroupLayout.makeHBox(GroupLayout.CENTER);
        cont.add(new BButton("New", this, "new_tourney"));
        cont.add(new BButton("Dismiss", this, "dismiss"));
        add(cont, GroupLayout.FIXED);

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

    /**
     * Called if there was a failure recieving the tournies object.
     */
    protected void showError ()
    {
    }

    protected class EntryBuilder
    {
        public EntryBuilder (TourneyListingEntry entry, boolean cache)
        {
            _entry = entry;

            if (cache) {
                _cachedEntry = build();
            } else {
                _cachedEntry = null;
            }
        }

        public BComponent build ()
        {
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

    protected class TourneyScrollList extends BScrollingList<EntryBuilder, BComponent>
    {
        public TourneyScrollList (Dimension size)
        {
            super();

            setPreferredSize(size);
        }

        @Override // from BScrollingList
        protected BComponent createComponent (EntryBuilder builder)
        {
            return builder.build();
        }
    }

    protected TourneyScrollList _scrollList;
    protected BangContext _ctx;
    protected SafeSubscriber<TourniesObject> _safesub;
    protected TourniesObject _tobj;
}
