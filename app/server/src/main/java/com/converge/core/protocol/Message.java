package com.converge.core.protocol;

import java.io.Serial;
import java.io.Serializable;

public class Message implements Serializable, Cloneable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
