package edu.illinois.library.cantaloupe.http;

import java.util.Objects;

/**
 * Immutable class encapsulating an HTTP header.
 */
public final class Header {

    private String name;
    private String value;

    public Header(String name, String value) {
        setName(name);
        setValue(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Header) {
            Header other = (Header) obj;
            return (Objects.equals(getName(), other.getName()) &&
                    Objects.equals(getValue(), other.getValue()));
        }
        return super.equals(obj);
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    private void setName(String name) {
        if (name == null || name.length() < 1) {
            throw new IllegalArgumentException("Illegal name: " + name);
        }
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    private void setValue(String value) {
        if (value == null || value.length() < 1) {
            throw new IllegalArgumentException("Illegal value: " + name);
        }
        this.value = value;
    }

    @Override
    public String toString() {
        return getValue();
    }

}
