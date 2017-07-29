//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.event;

import com.badlogic.gdx.Input.Keys;

/**
 * Encapsulates the information associated with a keyboard event.
 */
public class KeyEvent extends InputEvent
{
    /** Indicates that an event represents a key pressing. */
    public static final int KEY_PRESSED = 0;

    /** Indicates that an event represents a key being typed. This follows a pressed event for keys
     * that generate a character (unlike say, modifier keys). */
    public static final int KEY_TYPED = 1;

    /** Indicates that an event represents a key release. */
    public static final int KEY_RELEASED = 2;

    public KeyEvent (Object source, long when, int modifiers,
                     int type, char keyChar, int keyCode)
    {
        super(source, when, modifiers);
        _type = type;
        _keyChar = keyChar;
        _keyCode = keyCode;
    }

    /**
     * Indicates whether this was a {@link #KEY_PRESSED}, {@link #KEY_TYPED} or {@link
     * #KEY_RELEASED} event.
     */
    public int getType ()
    {
        return _type;
    }

    /**
     * Returns the character associated with the key. <em>Note:</em> this is only valid for {@link
     * #KEY_TYPED} events.
     */
    public char getKeyChar ()
    {
        // TEMP: This is a hack to get around a bug in lwjgl's handling of numpad keys in windows
        if (_keyChar == 0) {
            switch (_keyCode) {
            case Keys.NUMPAD_1: return '1';
            case Keys.NUMPAD_2: return '2';
            case Keys.NUMPAD_3: return '3';
            case Keys.NUMPAD_4: return '4';
            case Keys.NUMPAD_5: return '5';
            case Keys.NUMPAD_6: return '6';
            case Keys.NUMPAD_7: return '7';
            case Keys.NUMPAD_8: return '8';
            case Keys.NUMPAD_9: return '9';
            case Keys.NUMPAD_0: return '0';
            default: return _keyChar;
            }
        }
        // END TEMP
        return _keyChar;
    }

    /**
     * Returns the numeric identifier associated with the key. Note: this only works for pressed
     * and release events (not typed events).
     */
    public int getKeyCode ()
    {
        return _keyCode;
    }

    // documentation inherited
    public void dispatch (ComponentListener listener)
    {
        super.dispatch(listener);
        switch (_type) {
        case KEY_PRESSED:
            if (listener instanceof KeyListener) {
                ((KeyListener)listener).keyPressed(this);
            }
            break;

        case KEY_TYPED:
            if (listener instanceof KeyListener) {
                ((KeyListener)listener).keyTyped(this);
            }
            break;

        case KEY_RELEASED:
            if (listener instanceof KeyListener) {
                ((KeyListener)listener).keyReleased(this);
            }
            break;
        }
    }

    protected void toString (StringBuffer buf)
    {
        super.toString(buf);
        buf.append(", type=").append(TYPES[_type]);
        buf.append(", char=").append(_keyChar);
        buf.append(", code=").append(_keyCode);
    }

    protected int _type;
    protected char _keyChar;
    protected int _keyCode;

    protected static String[] TYPES = { "pressed", "typed", "released" };
}
