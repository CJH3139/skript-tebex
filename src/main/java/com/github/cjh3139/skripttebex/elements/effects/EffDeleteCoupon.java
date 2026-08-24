package com.github.cjh3139.skripttebex.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.AsyncEffect;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.Coupon;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletionException;

public class EffDeleteCoupon extends AsyncEffect {
    static {
        Skript.registerEffect(EffDeleteCoupon.class, "delete [the] tebex coupon %tebexcoupon%");
    }

    private Expression<?> couponExpression;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        getParser().setHasDelayBefore(Kleenean.TRUE);
        couponExpression = expressions[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null || !service.isConnected() || service.pluginApi() == null) {
            SkriptTebex.getInstance().logger().warn("Cannot delete a tebex coupon: Tebex is not connected.");
            return;
        }

        Object rawCoupon = couponExpression.getSingle(event);
        if (!(rawCoupon instanceof Coupon coupon)) return;

        try {
            service.pluginApi().deleteCoupon(coupon.id()).join();
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            SkriptTebex.getInstance().logger().warn("Failed to delete a Tebex coupon: {}", cause.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "delete a tebex coupon";
    }
}
