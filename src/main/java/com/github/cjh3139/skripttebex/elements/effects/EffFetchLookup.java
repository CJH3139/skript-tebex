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
import com.github.cjh3139.skripttebex.api.model.PlayerLookup;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletionException;

@Name("Fetch Tebex Player Lookup")
@Description(
    "Looks up a player's Tebex purchase history and stores the lookup in a variable. Accepts a player or a " +
    "username. This effect waits for the Tebex API and therefore delays the script."
)
@Examples("fetch the tebex lookup of player and store it in {_lookup}")
@Since("1.0.0")
public class EffFetchLookup extends AsyncEffect {
    static {
        Skript.registerEffect(EffFetchLookup.class,
            "fetch [the] tebex lookup of %player/string% and store (it|the result) in %~object%");
    }

    private Expression<?> playerExpression;
    private Variable<?> target;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        getParser().setHasDelayBefore(Kleenean.TRUE);
        playerExpression = expressions[0];
        if (!(expressions[1] instanceof Variable<?> variable)) {
            Skript.error("The result of a tebex lookup must be stored in a variable.");
            return false;
        }
        target = variable;
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null || !service.isConnected() || service.pluginApi() == null) {
            SkriptTebex.getInstance().logger().warn("Cannot fetch a tebex lookup: Tebex is not connected.");
            return;
        }

        Object rawPlayer = playerExpression.getSingle(event);
        if (rawPlayer == null) return;
        String user = rawPlayer instanceof Player player ? player.getUsername() : rawPlayer.toString();

        try {
            PlayerLookup lookup = service.pluginApi().lookup(user).join();
            target.change(event, new Object[]{lookup}, ChangeMode.SET);
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            SkriptTebex.getInstance().logger().warn("Failed to fetch a Tebex lookup: {}", cause.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "fetch a tebex lookup";
    }
}
