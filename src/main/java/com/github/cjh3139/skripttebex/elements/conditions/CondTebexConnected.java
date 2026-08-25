package com.github.cjh3139.skripttebex.elements.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.TebexService;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Tebex Is Connected")
@Description("Checks whether skript-tebex is authenticated with Tebex and polling the command queue.")
@Examples("if tebex is connected:\n\tbroadcast \"Store is online\"")
@Since("1.0.0")
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
