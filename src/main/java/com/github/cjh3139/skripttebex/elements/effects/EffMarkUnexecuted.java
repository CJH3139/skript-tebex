package com.github.cjh3139.skripttebex.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.elements.events.TebexCommandEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class EffMarkUnexecuted extends Effect {
    static {
        Skript.registerEffect(EffMarkUnexecuted.class, "mark [the] [event-]tebex command as unexecuted");
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        if (!getParser().isCurrentEvent(TebexCommandEvent.class)) {
            Skript.error("'mark the tebex command as unexecuted' can only be used in a tebex command event.");
            return false;
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (event instanceof TebexCommandEvent commandEvent) {
            commandEvent.setMarkExecuted(false);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "mark the tebex command as unexecuted";
    }
}
