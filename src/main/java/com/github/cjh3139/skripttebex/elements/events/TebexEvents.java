package com.github.cjh3139.skripttebex.elements.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;

public class TebexEvents {
    static {
        Skript.registerEvent("Tebex Purchase", SimpleEvent.class, TebexPurchaseEvent.class,
                "tebex purchase")
            .description("Called when a Tebex payment's commands are about to be delivered.")
            .since("1.0.0")
            .examples("on tebex purchase:\n\tbroadcast \"%event-player% bought %package%!\"");

        Skript.registerEvent("Tebex Command", SimpleEvent.class, TebexCommandEvent.class,
                "tebex command")
            .description("""
                Called for each command Tebex wants executed. Cancelling stops skript-tebex from \
                running it, but the command is still marked as executed with Tebex unless you use \
                'mark the tebex command as unexecuted'.""")
            .since("1.0.0")
            .examples("on tebex command:\n\tif command contains \"give\":\n\t\tcancel event");

        Skript.registerEvent("Tebex Queue Check", SimpleEvent.class, TebexQueueCheckEvent.class,
                "tebex queue check")
            .description("Called after each successful poll of the Tebex command queue.")
            .since("1.0.0")
            .examples("on tebex queue check:\n\tbroadcast \"%due players% players have purchases waiting\"");

        Skript.registerEvent("Tebex Queue Error", SimpleEvent.class, TebexQueueErrorEvent.class,
                "tebex queue error")
            .description("Called when a Tebex API request fails.")
            .since("1.0.0")
            .examples("on tebex queue error:\n\tsend \"Tebex error: %reason%\" to console");

        Skript.registerEvent("Tebex Connect", SimpleEvent.class, TebexConnectEvent.class,
                "tebex connect")
            .description("Called when skript-tebex authenticates with Tebex and loads store information.")
            .since("1.0.0")
            .examples("on tebex connect:\n\tbroadcast \"Connected to %tebex store name%\"");

        Skript.registerEvent("Tebex Disconnect", SimpleEvent.class, TebexDisconnectEvent.class,
                "tebex disconnect")
            .description("Called when skript-tebex stops polling, for example after the secret key is cleared.")
            .since("1.0.0")
            .examples("on tebex disconnect:\n\tsend \"Tebex polling stopped\" to console");
    }
}
