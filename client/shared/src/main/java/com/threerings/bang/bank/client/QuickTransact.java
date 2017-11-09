//
// $Id$

package com.threerings.bang.bank.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.layout.GroupLayout;
import com.samskivert.util.StringUtil;
import com.threerings.bang.admin.data.BuyOffer;
import com.threerings.bang.admin.data.SellOffer;
import com.threerings.bang.bank.data.BankCodes;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.ConsolidatedOffer;
import com.threerings.bang.data.Wallet;
import com.threerings.bang.util.BangContext;
import com.threerings.presents.client.InvocationService;
import com.threerings.util.MessageBundle;

/**
 * Displays an interface for making a quick transaction: an immediately
 * executed buy or sell order.
 */
public class QuickTransact extends BContainer
    implements ActionListener, BankCodes
{

    private ConsolidatedOffer offerToUse;
    public QuickTransact (BangContext ctx, StatusLabel status, boolean buying, Wallet wallet)
    {
        super(GroupLayout.makeHStretch());
        _ctx = ctx;
        _status = status;
        _buying = buying;
        _wallet = wallet;
        _msgs = ctx.getMessageManager().getBundle(BANK_MSGS);

        String msg = buying ? "m.buy" : "m.sell";
        add(new BLabel(_msgs.get(msg)), GroupLayout.FIXED);
        add(new BLabel(BangUI.coinIcon), GroupLayout.FIXED);
        add(_coins = new BTextField(20), GroupLayout.FIXED);
        _coins.setPreferredWidth(30);
        _coins.addListener(_coinlist);
        add(new BLabel(_msgs.get("m.for")), GroupLayout.FIXED);
        add(_scrip = new BLabel(BangUI.scripIcon));
        _scrip.setIconTextGap(5);
        add(_trade = new BButton(_msgs.get(msg), this, "go"),
            GroupLayout.FIXED);
        _trade.setEnabled(false);
        
        // Use a client > server > client sec Ill show u exactly how


        offerToUse = new ConsolidatedOffer();
        if(_buying)
        {
            _ctx.getClient().requireService(PlayerService.class).serverTunnel(
                    "RuntimeConfig.goldToScripRate",
                    new InvocationService.ConfirmListener() {

                        @Override
                        public void requestFailed(String s) {
                            try {
                                offerToUse.price = Integer.parseInt(s);
                                offerToUse.volume = 1;
                            } catch(NumberFormatException ex) {
                                ex.printStackTrace();
                            }
                        }

                        @Override
                        public void requestProcessed() {
                            // Failed to get data
                            System.out.println("Unable to get price!");
                        }
                    });
        } else {
            _ctx.getClient().requireService(PlayerService.class).serverTunnel(
                    "RuntimeConfig.scripToGoldRate",
                    new InvocationService.ConfirmListener() {

                        @Override
                        public void requestFailed(String s) {
                            try {
                                offerToUse.price = Integer.parseInt(s);
                                offerToUse.volume = 1;
                            } catch(NumberFormatException ex) {
                                ex.printStackTrace();
                            }
                        }

                        @Override
                        public void requestProcessed() {
                            // Failed to get data
                            System.out.println("Unable to get price!");
                        }
                    });
        }
        //
        //Right, bu how do I get that data
//        this.offerToUse = buying ? RuntimeConfig
    }

   /* public void init (BestOffer boffer)
    {
        _boffer = boffer;
        _boffer.addListener(_updater);
    }*/

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        // this should never happen but better safe than sorry
        if (_ccount <= 0) {
            return;
        }

        // determine the best offer price
        if (offerToUse == null) {
            return;
        }

        if(_buying)
        {
            _ctx.getClient().requireService(PlayerService.class).serverTunnel(
                    new SellOffer(_ccount),
                    new InvocationService.ConfirmListener() {

                        @Override
                        public void requestFailed(String s) {
                            _coins.setText("");
                            _status.setStatus(s, true);
                            return;
                        }

                        @Override
                        public void requestProcessed() {
                            _coins.setText("");
                            _status.setStatus(_msgs.xlate("Unknown error occurred"), true);
                            return;
                        }
                    });
        } else {
            _ctx.getClient().requireService(PlayerService.class).serverTunnel(
                    new BuyOffer(_ccount),
                    new InvocationService.ConfirmListener() {

                        @Override
                        public void requestFailed(String s) {
                            _coins.setText("");
                            _status.setStatus(s, true);
                            return;
                        }

                        @Override
                        public void requestProcessed() {
                            _coins.setText("");
                            _status.setStatus("Unknown error occurred", true);
                            return;
                        }
                    });
        }


        /*BankService.ResultListener rl = new BankService.ResultListener() {
            public void requestProcessed (Object result) {
                _coins.setText("");
                _status.setStatus(_msgs.get("m.trans_completed"), true);
            }
            public void requestFailed (String reason) {
                _status.setStatus(_msgs.xlate(reason), true);
            }
        };*/


//        _boffer.postImmediateOffer(
//            _ctx.getClient(), _ccount, best.price, _buying, rl);
    }

    protected void coinsUpdated ()
    {
        // clear out the trade and only enable it if all is well
        clearTrade();

        if (StringUtil.isBlank(_coins.getText())) {
            _status.setStatus("", false);
            return;
        }

        try {
            _ccount = Integer.parseInt(_coins.getText());
            if (_ccount <= 0) {
                return;
            }

            // make sure we have a best offer
            if (offerToUse == null) {
                _status.setStatus(_msgs.get("m.no_offers"), false);
                return;
            }

        if(_ccount > 10_000) {
            //Dont let them go higher..
            _trade.setEnabled(false);
            _coins.setEnabled(false);
            _coins.setText("");
            _coins.setEnabled(true);
            _status.setStatus(_msgs.get("m.no_offers"), false);
            return;
        }

            _value = offerToUse.price * _ccount;

//            int maxToBuy = Integer.MAX_VALUE / offerToUse.price;

            if(_value < 0) {
//                System.out.println("Max: " + maxToBuy);
                _coins.setText(0 + "");
                return;
            }

            _scrip.setText(String.valueOf(_value));

            // make sure they have sufficient funds
            if (_buying && _ccount * offerToUse.price > _wallet.getScrip()) {
                _status.setStatus(_msgs.get("m.insufficient_scrip"), false);
                return;
            } else if (!_buying && _ccount > _wallet.getCoins()) {
                _status.setStatus(_msgs.get("m.insufficient_coins"), false);
                return;
            }

            _trade.setEnabled(true);
            _status.setStatus("", false);

        } catch (Exception e) {
            // just leave the button disabled as they entered a bogus value
        }
    }

    protected void clearTrade ()
    {
        _scrip.setText("");
        _trade.setEnabled(false);
        _ccount = -1;
        _value = 0;
    }

    protected TextListener _coinlist = new TextListener() {
        public void textChanged (TextEvent event) {
            coinsUpdated();
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
//    protected BestOffer _boffer;

    protected boolean _buying;
    protected int _ccount, _value;
    protected Wallet _wallet;

    protected StatusLabel _status;
    protected BTextField _coins;
    protected BLabel _scrip;
    protected BButton _trade;
}
