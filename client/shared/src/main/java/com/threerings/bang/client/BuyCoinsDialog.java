//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.*;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.LengthLimitedDocument;
import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.RequestDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.NameFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Allows Support to warn a player
 */
public class BuyCoinsDialog extends SteelWindow implements ActionListener
{
    public BuyCoinsDialog(
        BangContext ctx)
    {
        super(ctx, "Buy Coins");

        _ctx = ctx;
        setModal(true);
        _contents.setLayoutManager(new BorderLayout());
        ((BorderLayout)_contents.getLayoutManager()).setGaps(50, 20);
        String[] availablePackages = {};
        _contents.add(packageList = new BComboBox(availablePackages));
        packageList.addListener(this);
        _buttons.add(new BButton("Dismiss", this, "dismiss"));


    }

    protected BangContext _ctx;
    protected BComboBox packageList;

    @Override
    public void actionPerformed(ActionEvent event) {
        if(event.getSource() == packageList) {
            try {
                _ctx.showURL(new URL("http://banghowdy.com/purchase/"));
            } catch (MalformedURLException e) {
                _ctx.getBangClient().clearPopup(this, true);
                return;
            }
            return;
        }
        switch (event.getAction()) {
            case "dismiss":
                _ctx.getBangClient().clearPopup(this, true);
                break;
        }
    }
}
