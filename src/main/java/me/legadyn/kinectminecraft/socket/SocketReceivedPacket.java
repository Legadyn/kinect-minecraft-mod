package me.legadyn.kinectminecraft.socket;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class SocketReceivedPacket {

    public static final Event<MyCustomEventListener> EVENT = EventFactory.createArrayBacked(MyCustomEventListener.class,
            (listeners) -> (myCustomArg) -> {
                for (MyCustomEventListener listener : listeners) {
                    boolean eventHandled = listener.onMyCustomEvent(myCustomArg);
                    if (eventHandled) {
                        return true;
                    }
                }
                return false;
            });

    public interface MyCustomEventListener {
        boolean onMyCustomEvent(String myCustomArg);
    }
}
