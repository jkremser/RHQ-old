package org.rhq.plugins.RHEVM;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.pluginapi.event.EventPoller;


public class RHEVMEventPoller implements EventPoller {

    public RHEVMEventPoller() {

    }


    /** Return the type of events we handle
     * @see org.rhq.core.pluginapi.event.EventPoller#getEventType()
     */
    public String getEventType() {
        return RHEVMComponent.DUMMY_EVENT;
    }


    /** Return collected events
     * @see org.rhq.core.pluginapi.event.EventPoller#poll()
     */
    public Set<Event> poll() {
        Set<Event> eventSet = new HashSet<Event>();
        // TODO add your events here. Below is an example that
        /*
        synchronized (events) {
            eventSet.addAll(events);
            events.clear();
        }
        */
        return eventSet;
    }

}