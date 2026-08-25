package com.github.cjh3139.skripttebex.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.elements.events.TebexCommandEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Mark Tebex Command As Unexecuted")
@Description(
    "Marks the command of a tebex command event as unexecuted, so Tebex delivers it again on a later queue check. " +
    "Can only be used in a tebex command event."
)
@Examples("on tebex command:\n\tif player is not online:\n\t\tmark the tebex command as unexecuted")
@Since("1.0.0")
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
