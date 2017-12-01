//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Point;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.steam.SteamStorage;
import com.threerings.bang.util.BangContext;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Buy Coins Dialog
 */
public class BuyCoinsDialog extends BDecoratedWindow implements ActionListener {
    protected BangContext _ctx;

    public BuyCoinsDialog(
            BangContext ctx) {
        super(ctx.getStyleSheet(), "Buy Coins");
        ((GroupLayout)getLayoutManager()).setOffAxisPolicy(GroupLayout.STRETCH);
        setModal(true);
        setLayer(BangCodes.NEVER_CLEAR_LAYER);
        _ctx = ctx;
        setLayoutManager(GroupLayout.makeVert(GroupLayout.LEFT).setGap(5));
        setSize((int)(_ctx.getDisplay().getWidth() * .75), (int)(ctx.getDisplay().getHeight() * .5));
        BContainer container = new BContainer(GroupLayout.makeHStretch());
        container.setBounds(container.getX(), container.getY(), (int) (_ctx.getDisplay().getWidth()  * .75), (int) (ctx.getDisplay().getHeight() * .5));

        for (CoinPackage coinPackage : CoinPackage.values()) {
            BContainer packageContainer = new BContainer(GroupLayout.makeVert(GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.NONE));
            int width = (container.getWidth() - 40) / CoinPackage.values().length;
            packageContainer.setStyleClass("tooltip_window");


            Point lastPoint;
            BLabel label;
            packageContainer.add(label = new BLabel(coinPackage.getName()), GroupLayout.FIXED);
            ImageIcon icon;
            packageContainer.add(new BLabel( new ImageIcon(ctx.loadImage("ui/buttons/massive_down.png"))), GroupLayout.FIXED);

            packageContainer.add(new BLabel(coinPackage.getCoinAmount() + " Coins"), GroupLayout.FIXED);
            BButton butt = new BButton("$" + coinPackage.getPrice(), this, String.valueOf(coinPackage.getPackageID()));
            packageContainer.add(butt, GroupLayout.FIXED);
            butt.setProperty("feedback_sound", BangUI.FeedbackSound.ITEM_PURCHASE);

            container.add(packageContainer, GroupLayout.FIXED);
        }

        add(container, GroupLayout.FIXED);


        add(new BButton("Dismiss", this, "dismiss"));
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        for (CoinPackage clicked : CoinPackage.values()) {
            if (event.getAction().equals(clicked.getPackageID() + "")) {
                //Clicked buy this package?
                try {
                    _ctx.showURL(new URL("https://id.yourfunworld.com/buy.php?purchase=" + clicked.getPackageID() + (SteamStorage.user != null ? "&id=" + SteamStorage.user.getSteamID().getAccountID() : "")));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return;



            }
        }

        switch (event.getAction()) {
            case "dismiss":
                _ctx.getBangClient().clearPopup(this, true);
                break;
        }
    }


    private enum CoinPackage {
        SMALL("Bag O' Coins", 1, 3, 0.99),
        MEDIUM("Pile O' Coins", 2, 40, 9.99), //worth double
        LARGE("Bucket O' Coins", 3, 100, 19.99),
        EXTRALARGE("Wagon O' Coins", 4, 500, 99.99);

        private String name;
        private int packageID, coinAmount;
        private double price;

        CoinPackage(String name, int packageID, int coinAmount, double price) {
            this.name = name;
            this.packageID = packageID;
            this.coinAmount = coinAmount;
            this.price = price;
        }

        public String getName() {
            return name;
        }

        public int getCoinAmount() {
            return coinAmount;
        }

        public double getPrice() {
            return price;
        }

        public int getPackageID() {
            return packageID;
        }

    }
}
