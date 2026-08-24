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
import com.github.cjh3139.skripttebex.api.model.Sale;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletionException;

public class EffFetchSales extends AsyncEffect {
    static {
        Skript.registerEffect(EffFetchSales.class,
            "fetch [the] tebex sales and store (them|the result) in %~objects%");
    }

    private Variable<?> target;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        getParser().setHasDelayBefore(Kleenean.TRUE);
        if (!(expressions[0] instanceof Variable<?> variable)) {
            Skript.error("The result of tebex sales must be stored in a variable.");
            return false;
        }
        if (variable.isSingle()) {
            Skript.error("The result of tebex sales must be stored in a list variable, e.g. {_sales::*}.");
            return false;
        }
        target = variable;
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null || !service.isConnected() || service.pluginApi() == null) {
            SkriptTebex.getInstance().logger().warn("Cannot fetch tebex sales: Tebex is not connected.");
            return;
        }

        try {
            List<Sale> sales = service.pluginApi().sales().join();
            target.change(event, sales.toArray(new Sale[0]), ChangeMode.SET);
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            SkriptTebex.getInstance().logger().warn("Failed to fetch Tebex sales: {}", cause.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "fetch tebex sales";
    }
}
