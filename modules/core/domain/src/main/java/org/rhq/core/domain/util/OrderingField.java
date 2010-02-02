 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.domain.util;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Object to hold an ORDER BY field argument including the name and sort order.
 */
public class OrderingField implements Serializable {
    private static final long serialVersionUID = 1L;

    private String field;
    private PageOrdering ordering;

    protected OrderingField() {
    }

    public OrderingField(String field, PageOrdering ordering) {
        this.field = field;
        this.ordering = (ordering != null) ? ordering : PageOrdering.ASC;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        if (field == null) {
            throw new IllegalArgumentException("OrderingField.field can not be null");
        }

        this.field = field;
    }

    @NotNull
    public PageOrdering getOrdering() {
        return ordering;
    }

    public void setOrdering(PageOrdering ordering) {
        if (ordering == null) {
            throw new IllegalArgumentException("OrderingField.ordering can not be null");
        }

        this.ordering = ordering;
    }

    public void flipOrdering() {
        if (ordering == PageOrdering.ASC) {
            ordering = PageOrdering.DESC;
        } else {
            ordering = PageOrdering.ASC;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrderingField that = (OrderingField) o;

        if (!field.equals(that.field)) return false;
        if (ordering != that.ordering) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = field.hashCode();
        result = 31 * result + ordering.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[field=" + this.field + ", ordering=" + this.ordering + "]";
    }
}