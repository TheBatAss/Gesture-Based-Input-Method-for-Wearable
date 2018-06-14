package com.mbientlab.metawear.tutorial.starter;

import com.mbientlab.metawear.Data;

/**
 * Created by andersbeck on 30/04/2018.
 */

public class dataVariable {
    private Data data;
    private ChangeListener listener;

    public Data isData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
        if (listener != null) listener.onChange();
    }

    public ChangeListener getListener() {
        return listener;
    }

    public void setListener(ChangeListener listener) {
        this.listener = listener;
    }

    public interface ChangeListener {
        void onChange();
    }
}
