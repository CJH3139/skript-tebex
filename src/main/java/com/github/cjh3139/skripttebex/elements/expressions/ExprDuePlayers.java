package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.QueuedPlayer;
import com.github.cjh3139.skripttebex.queue.CommandQueue;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprDuePlayers extends SimpleExpression<QueuedPlayer> {
    static {
        Skript.registerExpression(ExprDuePlayers.class, QueuedPlayer.class, ExpressionType.SIMPLE,
            "[the] tebex due players");
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        return true;
    }

    @Override
    protected QueuedPlayer @Nullable [] get(Event event) {
        TebexService service = TebexService.get();
        if (service == null) return new QueuedPlayer[0];
        CommandQueue queue = service.queue();
        if (queue == null) return new QueuedPlayer[0];
        return queue.duePlayers().toArray(QueuedPlayer[]::new);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends QueuedPlayer> getReturnType() {
        return QueuedPlayer.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "tebex due players";
    }
}
