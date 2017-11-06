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
        ((GroupLayout) getLayoutManager()).setOffAxisPolicy(GroupLayout.STRETCH);
        setModal(true);
        setLayer(BangCodes.NEVER_CLEAR_LAYER);
        _ctx = ctx;
        setLayoutManager(GroupLayout.makeVert(GroupLayout.LEFT).setGap(5));

        BContainer container = new BContainer(GroupLayout.makeHStretch());
        container.setBounds(container.getX(), container.getY(), (int) (_ctx.getDisplay().getWidth()  * .75), (int) (ctx.getDisplay().getHeight() * .5));
//        container.setW
//        container.add(GroupLayout.makeHBox(GroupLayout.CENTER));

        //10px?
        for (CoinPackage coinPackage : CoinPackage.values()) {
            BContainer packageContainer = new BContainer(GroupLayout.makeVert(GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.NONE));
//            packageContainer.setPreferredSize(container.getWidth() / CoinPackage.values().length, container.getHeight());
//            packageContainer.setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER).setGap(15));
//            packageContainer.setPreferredSize((container.getWidth() - 40) / CoinPackage.values().length , (int) (container.getHeight() * .4));

            int width = (container.getWidth() - 40) / CoinPackage.values().length;
//            packageContainer.setBounds(packageContainer.getX(), packageContainer.getY(), width, (int) (container.getHeight() * .5));
//            System.out.println("Size: " + packageContainer.getHeight() + " Width " + packageContainer.getWidth() + " Container: " + container.getWidth() + " h: " + container.getHeight() + " displ: " + _ctx.getDisplay().getWidth());
//            TableLayout layout = new TableLayout(1, 10, 10);
//            layout.setHorizontalAlignment(TableLayout.CENTER);
//            layout.setVerticalAlignment(TableLayout.CENTER);
//            packageContainer.setLayoutManager(layout);

//            packageContainer.setLayoutManager(new AbsoluteLayout());
            packageContainer.setStyleClass("tooltip_window");


            Point lastPoint;
            BLabel label;
            packageContainer.add(label = new BLabel(coinPackage.getName()), GroupLayout.FIXED);
            ImageIcon icon;
//            packageContainer.add(new BLabel(icon = new ImageIcon(ctx.loadImage("ui/icons/coins.png"))), lastPoint = new Point((packageContainer.getWidth() / 2) - icon.getWidth() / 2, lastPoint.y - 45));

//            BContainer coinAndPriceContainer = GroupLayout.makeHBox(GroupLayout.CENTER);
//            TableLayout layout = new TableLayout(1, 10, 10);
//            layout.setHorizontalAlignment(TableLayout.CENTER);
//            coinAndPriceContainer.setLayoutManager(layout);

//            coinAndPriceContainer.setLayoutManager(GroupLayout.makeVert(GroupLayout.STRETCH, GroupLayout.CENTER,
//                    GroupLayout.CONSTRAIN).setOffAxisJustification(GroupLayout.CENTER));
//            ((GroupLayout)coinAndPriceContainer.getLayoutManager()).setGap(10);
//            coinAndPriceContainer.setBounds(0, 0, width, packageContainer.getHeight() / 4);


            packageContainer.add(new BLabel( new ImageIcon(ctx.loadImage("ui/icons/coins.png")), coinPackage.getCoinAmount() + " Coins"), GroupLayout.FIXED);
            BButton butt = new BButton("$" + coinPackage.getPrice(), this, String.valueOf(coinPackage.getPackageID()));
            packageContainer.add(butt, GroupLayout.FIXED);

            //coinAndPriceContainer.add(new BLabel(coinPackage.getCoinAmount() + " Coins"), lastPoint = new Point((packageContainer.getWidth() / 2) - 30, 50));
            //BButton butt = new BButton("$" + coinPackage.getPrice(), this, String.valueOf(coinPackage.getPackageID()));
            //coinAndPriceContainer.add(butt, new Point(12, 5));

//            packageContainer.add(coinAndPriceContainer, new Point(packageContainer.getWidth() / 4, 10));
            System.out.println("Size: " + packageContainer.getHeight() + " Width " + packageContainer.getWidth() + " Container: " + container.getWidth() + " h: " + container.getHeight() + " displ: " + _ctx.getDisplay().getWidth() + " but width: " + butt.getWidth());

            /*
            BLabel label;
            packageContainer.add(label = new BLabel(coinPackage.getName()));
            ImageIcon icon;
            packageContainer.add(new BLabel(icon = new ImageIcon(ctx.loadImage("ui/icons/coins.png"))));

//            packageContainer.add(new BLabel(""), new Rectangle(0, 0, packageContainer.getWidth(), (int) (packageContainer.getHeight() * .3)));

            packageContainer.add(new BLabel(coinPackage.getCoinAmount() + " Coins"), TableLayout.BOTTOM);

            BButton butt = new BButton("$" + coinPackage.getPrice(), this, String.valueOf(coinPackage.getPackageID()));
            packageContainer.add(butt, TableLayout.BOTTOM);*/
            butt.setProperty("feedback_sound", BangUI.FeedbackSound.ITEM_PURCHASE);

            container.add(packageContainer);
        }

        add(container);


        add(new BButton("Dismiss", this, "dismiss"));
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        for (CoinPackage clicked : CoinPackage.values()) {
            if (event.getAction().equals(clicked.getPackageID() + "")) {
                //Clicked buy this package?
                try {
                    _ctx.showURL(new URL("https://banghowdy.com/buy.php?purchase=" + clicked.getPackageID()));
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
