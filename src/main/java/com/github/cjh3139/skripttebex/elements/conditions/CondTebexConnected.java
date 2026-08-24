package com.github.cjh3139.skripttebex.elements.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.TebexService;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class CondTebexConnected extends Condition {
    static {
        Skript.registerCondition(CondTebexConnected.class,
            "tebex is connected",
            "tebex is(n't| not) connected");
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        TebexService service = TebexService.get();
        return isNegated() != (service != null && service.isConnected());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "tebex is " + (isNegated() ? "not " : "") + "connected";
    }
}
