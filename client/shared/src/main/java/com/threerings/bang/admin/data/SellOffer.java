package com.threerings.bang.admin.data;

import com.threerings.presents.dobj.DObject;

import java.io.Serializable;

public class SellOffer extends DObject implements Serializable {

    public int storedoffer = 0;

    public SellOffer(int offer)
    {
        storedoffer = offer;
    }


}
