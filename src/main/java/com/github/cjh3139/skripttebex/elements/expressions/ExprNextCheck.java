package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.queue.CommandQueue;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Seconds Until Next Tebex Check")
@Description("The number of seconds until the next poll of the Tebex command queue. Returns 0 when the queue is not running.")
@Examples("send \"Next check in %seconds until the next tebex check% seconds\" to player")
@Since("1.0.0")
public class ExprNextCheck extends SimpleExpression<Number> {
    static {
        Skript.registerExpression(ExprNextCheck.class, Number.class, ExpressionType.SIMPLE,
            "[the] seconds until [the] next tebex check");
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        return true;
    }

    @Override
    protected Number @Nullable [] get(Event event) {
        TebexService service = TebexService.get();
        CommandQueue queue = service == null ? null : service.queue();
        int seconds = queue == null ? 0 : queue.nextCheckSeconds();
        return new Number[]{seconds};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "seconds until next tebex check";
    }
}
