package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.QueuedCommand;
import com.github.cjh3139.skripttebex.queue.CommandQueue;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ExprPendingCommands extends SimpleExpression<QueuedCommand> {
    static {
        Skript.registerExpression(ExprPendingCommands.class, QueuedCommand.class, ExpressionType.SIMPLE,
            "[the] pending tebex commands of %player%");
    }

    private Expression<Player> playerExpr;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        playerExpr = (Expression<Player>) expressions[0];
        return true;
    }

    @Override
    protected QueuedCommand @Nullable [] get(Event event) {
        TebexService service = TebexService.get();
        if (service == null) return new QueuedCommand[0];
        CommandQueue queue = service.queue();
        if (queue == null) return new QueuedCommand[0];

        List<QueuedCommand> commands = new ArrayList<>();
        for (Player player : playerExpr.getArray(event)) {
            String usernameId = player.getUuid().toString().replace("-", "");
            commands.addAll(queue.deferredFor(usernameId));
        }
        return commands.toArray(new QueuedCommand[0]);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends QueuedCommand> getReturnType() {
        return QueuedCommand.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "pending tebex commands of " + playerExpr.toString(event, debug);
    }
}
