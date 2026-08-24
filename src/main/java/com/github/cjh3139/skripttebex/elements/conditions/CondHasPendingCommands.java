package com.github.cjh3139.skripttebex.elements.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.queue.CommandQueue;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class CondHasPendingCommands extends Condition {
    static {
        Skript.registerCondition(CondHasPendingCommands.class,
            "%player% has [any] pending tebex commands",
            "%player% does(n't| not) have [any] pending tebex commands");
    }

    private Expression<Player> players;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        players = (Expression<Player>) expressions[0];
        setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        return players.check(event, this::hasPendingCommands, isNegated());
    }

    private boolean hasPendingCommands(Player player) {
        TebexService service = TebexService.get();
        if (service == null) return false;
        CommandQueue queue = service.queue();
        if (queue == null) return false;
        String usernameId = player.getUuid().toString().replace("-", "");
        return !queue.deferredFor(usernameId).isEmpty();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return players.toString(event, debug) + (isNegated() ? " does not have " : " has ") + "pending tebex commands";
    }
}
