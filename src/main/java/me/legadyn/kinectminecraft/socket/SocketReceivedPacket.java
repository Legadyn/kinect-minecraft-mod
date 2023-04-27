package me.legadyn.kinectminecraft.socket;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;

public class SocketReceivedPacket {

    public static final Event<MyCustomEventListener> EVENT = EventFactory.createArrayBacked(MyCustomEventListener.class,
            (listeners) -> (myCustomArg) -> {
                for (MyCustomEventListener listener : listeners) {
                    ActionResult result = listener.onMyCustomEvent(myCustomArg);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public interface MyCustomEventListener {
        ActionResult onMyCustomEvent(String myCustomArg);
    }
}
