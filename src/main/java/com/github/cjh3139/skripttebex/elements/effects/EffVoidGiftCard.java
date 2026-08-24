package com.github.cjh3139.skripttebex.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.AsyncEffect;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.GiftCard;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletionException;

public class EffVoidGiftCard extends AsyncEffect {
    static {
        Skript.registerEffect(EffVoidGiftCard.class, "void [the] tebex giftcard %tebexgiftcard%");
    }

    private Expression<?> giftCardExpression;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        getParser().setHasDelayBefore(Kleenean.TRUE);
        giftCardExpression = expressions[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null || !service.isConnected() || service.pluginApi() == null) {
            SkriptTebex.getInstance().logger().warn("Cannot void a tebex gift card: Tebex is not connected.");
            return;
        }

        Object rawGiftCard = giftCardExpression.getSingle(event);
        if (!(rawGiftCard instanceof GiftCard giftCard)) return;

        try {
            service.pluginApi().voidGiftCard(giftCard.id()).join();
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            SkriptTebex.getInstance().logger().warn("Failed to void a Tebex gift card: {}", cause.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "void a tebex gift card";
    }
}
