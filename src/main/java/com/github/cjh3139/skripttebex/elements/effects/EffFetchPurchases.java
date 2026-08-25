package com.github.cjh3139.skripttebex.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.util.AsyncEffect;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.CustomerPackage;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletionException;

@Name("Fetch Tebex Purchases")
@Description(
    "Fetches the packages a player has purchased into a list variable. Accepts a player or a username. This effect " +
    "waits for the Tebex API and therefore delays the script."
)
@Examples("fetch the tebex purchases of player and store them in {_purchases::*}")
@Since("1.0.0")
public class EffFetchPurchases extends AsyncEffect {
    static {
        Skript.registerEffect(EffFetchPurchases.class,
            "fetch [the] tebex purchases of %player/string% and store (it|them|the result) in %~objects%");
    }

    private Expression<?> playerExpression;
    private Variable<?> target;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        getParser().setHasDelayBefore(Kleenean.TRUE);
        playerExpression = expressions[0];
        if (!(expressions[1] instanceof Variable<?> variable)) {
            Skript.error("The result of tebex purchases must be stored in a variable.");
            return false;
        }
        if (variable.isSingle()) {
            Skript.error("The result of tebex purchases must be stored in a list variable, e.g. {_purchases::*}.");
            return false;
        }
        target = variable;
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null || !service.isConnected() || service.pluginApi() == null) {
            SkriptTebex.getInstance().logger().warn("Cannot fetch tebex purchases: Tebex is not connected.");
            return;
        }

        Object rawPlayer = playerExpression.getSingle(event);
        if (rawPlayer == null) return;

        String usernameId = rawPlayer instanceof Player player
            ? player.getUuid().toString().replace("-", "")
            : rawPlayer.toString();

        try {
            List<CustomerPackage> purchases = service.pluginApi().customerPackages(usernameId).join();
            target.change(event, purchases.toArray(new CustomerPackage[0]), ChangeMode.SET);
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            SkriptTebex.getInstance().logger().warn("Failed to fetch Tebex purchases: {}", cause.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "fetch tebex purchases";
    }
}
