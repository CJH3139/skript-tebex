package com.github.cjh3139.skripttebex.elements.effects;

import ch.njol.skript.Skript;
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
import com.github.cjh3139.skripttebex.api.model.CheckoutUrl;
import com.github.cjh3139.skripttebex.api.model.Package;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletionException;

@Name("Create Tebex Checkout Link")
@Description(
    "Creates a checkout link for a single package and stores the URL in a variable. Accepts a tebex package or a " +
    "package id, and a player or a username. This effect waits for the Tebex API and therefore delays the script."
)
@Examples("create a tebex checkout link for {_package} for player and store it in {_url}\n\tsend {_url} to player")
@Since("1.0.0")
public class EffCheckoutLink extends AsyncEffect {
    static {
        Skript.registerEffect(EffCheckoutLink.class,
            "create [a] tebex checkout link for %tebexpackage/number% for %player/string% and store (it|the result) in %~object%");
    }

    private Expression<?> packageExpression;
    private Expression<?> playerExpression;
    private Variable<?> target;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        getParser().setHasDelayBefore(Kleenean.TRUE);
        packageExpression = expressions[0];
        playerExpression = expressions[1];
        if (!(expressions[2] instanceof Variable<?> variable)) {
            Skript.error("The result of a tebex checkout link must be stored in a variable.");
            return false;
        }
        target = variable;
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null || !service.isConnected() || service.pluginApi() == null) {
            SkriptTebex.getInstance().logger().warn("Cannot create a checkout link: Tebex is not connected.");
            return;
        }

        Object rawPackage = packageExpression.getSingle(event);
        Object rawPlayer = playerExpression.getSingle(event);
        if (rawPackage == null || rawPlayer == null) return;

        int packageId = rawPackage instanceof Package pkg ? pkg.id() : ((Number) rawPackage).intValue();
        String username = rawPlayer instanceof Player player ? player.getUsername() : rawPlayer.toString();

        try {
            CheckoutUrl checkout = service.pluginApi().checkout(packageId, username).join();
            target.change(event, new Object[]{checkout.url()}, ch.njol.skript.classes.Changer.ChangeMode.SET);
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            SkriptTebex.getInstance().logger().warn("Failed to create a Tebex checkout link: {}", cause.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "create a tebex checkout link";
    }
}
