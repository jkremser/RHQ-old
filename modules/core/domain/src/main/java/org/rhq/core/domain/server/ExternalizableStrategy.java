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
package org.rhq.core.domain.server;

/**
 * This uses a ThreadLocal to bind an externalization strategy based on the invoking subsystem. In other
 * words, when we know we're serializing for Server-Agent communication then set to AGENT, when we know we're
 * serializing for RemoteClient-Server communication set to REMOTEAPI.  By keeping this info on the thread
 * we avoid having to tag all of the relevant objects that will be serialized. 
 *  
 * @author jay shaughnessy
 */
public class ExternalizableStrategy {

    public enum Subsystem {
        AGENT((char) 1), // set bidirectionally for agent<--->server communication
        REFLECTIVE_SERIALIZATION((char) 3); // set unidirectionally for both CLI-->server and WS-->server communication

        private char id;

        Subsystem(char id) {
            this.id = id;
        }

        public char id() {
            return id;
        }
    }

    private static ThreadLocal<Subsystem> strategy = new ThreadLocal<Subsystem>() {

        protected ExternalizableStrategy.Subsystem initialValue() {
            return Subsystem.AGENT;
        }
    };

    public static Subsystem getStrategy() {
        return strategy.get();
    }

    public static void setStrategy(Subsystem newStrategy) {
        strategy.set(newStrategy);
    }
}
