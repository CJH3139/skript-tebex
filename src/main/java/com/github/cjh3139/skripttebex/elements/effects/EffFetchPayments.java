package com.github.cjh3139.skripttebex.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.util.AsyncEffect;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.Payment;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletionException;

public class EffFetchPayments extends AsyncEffect {
    private static final int DEFAULT_LIMIT = 10;

    static {
        Skript.registerEffect(EffFetchPayments.class,
            "fetch [the] [last %-number%] tebex payments and store (them|the result) in %~objects%");
    }

    @Nullable
    private Expression<?> limitExpression;
    private Variable<?> target;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        getParser().setHasDelayBefore(Kleenean.TRUE);
        limitExpression = expressions[0];
        if (!(expressions[1] instanceof Variable<?> variable)) {
            Skript.error("The result of tebex payments must be stored in a variable.");
            return false;
        }
        if (variable.isSingle()) {
            Skript.error("The result of tebex payments must be stored in a list variable, e.g. {_payments::*}.");
            return false;
        }
        target = variable;
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null || !service.isConnected() || service.pluginApi() == null) {
            SkriptTebex.getInstance().logger().warn("Cannot fetch tebex payments: Tebex is not connected.");
            return;
        }

        int limit = DEFAULT_LIMIT;
        if (limitExpression != null) {
            Object rawLimit = limitExpression.getSingle(event);
            if (rawLimit instanceof Number number) limit = number.intValue();
        }

        try {
            List<Payment> payments = service.pluginApi().payments(limit).join();
            target.change(event, payments.toArray(new Payment[0]), ChangeMode.SET);
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            SkriptTebex.getInstance().logger().warn("Failed to fetch Tebex payments: {}", cause.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "fetch tebex payments";
    }
}
