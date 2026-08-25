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
import com.github.cjh3139.skripttebex.api.model.Package;
import com.github.cjh3139.skripttebex.util.BasketCheckout;
import com.github.cjh3139.skripttebex.util.BasketReturnUrl;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

@Name("Create Tebex Basket Link")
@Description(
    "Creates a Tebex Headless basket that contains the given packages for a player, then stores the basket's " +
    "checkout link in a variable. Requires a Headless public token to be configured. This effect waits for the " +
    "Tebex API and therefore delays the script."
)
@Examples("on command /cart:\n\tcreate a tebex basket link for {_packages::*} for player and store it in {_link}\n\tsend {_link} to player")
@Since("1.0.0")
public class EffBasketLink extends AsyncEffect {
    static {
        Skript.registerEffect(EffBasketLink.class,
            "create [a] tebex basket link for %tebexpackages% for %player% and store (it|the result) in %~object%");
    }

    private Expression<?> packagesExpression;
    private Expression<?> playerExpression;
    private Variable<?> target;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        getParser().setHasDelayBefore(Kleenean.TRUE);
        packagesExpression = expressions[0];
        playerExpression = expressions[1];
        if (!(expressions[2] instanceof Variable<?> variable)) {
            Skript.error("The result of a tebex basket link must be stored in a variable.");
            return false;
        }
        target = variable;
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null || !service.isConnected() || service.headlessApi() == null) {
            SkriptTebex.getInstance().logger()
                .warn("Cannot create a basket link: Tebex is not connected or has no public token configured.");
            return;
        }

        Object rawPlayer = playerExpression.getSingle(event);
        if (!(rawPlayer instanceof Player player)) return;
        Object[] rawPackages = packagesExpression.getArray(event);
        if (rawPackages.length == 0) return;

        List<Integer> packageIds = new ArrayList<>(rawPackages.length);
        for (Object rawPackage : rawPackages) {
            packageIds.add(((Package) rawPackage).id());
        }

        String username = player.getUsername();
        String usernameId = player.getUuid().toString().replace("-", "");
        String returnUrl = BasketReturnUrl.resolve(service);

        try {
            String checkout = BasketCheckout
                .checkoutLink(service.headlessApi(), username, usernameId, returnUrl, packageIds)
                .join();
            target.change(event, new Object[]{checkout}, ChangeMode.SET);
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            SkriptTebex.getInstance().logger().warn("Failed to create a Tebex basket link: {}", cause.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "create a tebex basket link";
    }
}
