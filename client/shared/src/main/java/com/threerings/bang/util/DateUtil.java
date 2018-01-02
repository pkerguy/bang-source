package com.threerings.bang.util;

import java.util.Date;

public class DateUtil {

    public static boolean isDateInBetween(final Date min, final Date end, final Date date)
    {
        return !(date.before(min) || date.after(end));
    }
    public static boolean isDateInBetween(final Date min, final Date end)
    {
        return isDateInBetween(min, end, new Date());
    }

}