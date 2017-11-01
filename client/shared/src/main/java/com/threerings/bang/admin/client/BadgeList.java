package com.threerings.bang.admin.client;

import com.jmex.bui.*;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.threerings.bang.data.Badge;
import jdk.nashorn.internal.objects.annotations.Getter;

public class BadgeList extends BScrollingList<BadgeList.EntryBuilder, BComponent>
{

    private String selected;

    public BadgeList (Dimension size)
    {
        super();

        setPreferredSize(size);
    }

    @Override // from BScrollingList
    protected BComponent createComponent (BadgeList.EntryBuilder entry)
    {
        return entry.build();
    }

    protected class EntryBuilder implements ActionListener {
        public EntryBuilder() {
        }

        public BComponent build() {

            BContainer cont = GroupLayout.makeHBox(GroupLayout.CENTER);
            for(Badge b : Badge.getAll())
            {
                cont.add(new BButton(b.getName(), this, String.valueOf(b.getCode())));
            }

            return cont;
        }

        @Override
        public void actionPerformed (ActionEvent event) {
            selected = event.getAction();
        }

    }
    public String getSelected(){
        return selected;
    }
}