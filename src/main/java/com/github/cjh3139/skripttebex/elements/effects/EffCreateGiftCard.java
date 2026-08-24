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
import com.github.cjh3139.skripttebex.api.model.GiftCard;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletionException;

public class EffCreateGiftCard extends AsyncEffect {
    static {
        Skript.registerEffect(EffCreateGiftCard.class,
            "create [a] tebex giftcard worth %number% [with note %-string%] and store (it|the result) in %~object%");
    }

    private Expression<?> amountExpression;
    @Nullable
    private Expression<?> noteExpression;
    private Variable<?> target;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        getParser().setHasDelayBefore(Kleenean.TRUE);
        amountExpression = expressions[0];
        noteExpression = expressions[1];
        if (!(expressions[2] instanceof Variable<?> variable)) {
            Skript.error("The result of a tebex gift card must be stored in a variable.");
            return false;
        }
        target = variable;
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null || !service.isConnected() || service.pluginApi() == null) {
            SkriptTebex.getInstance().logger().warn("Cannot create a tebex gift card: Tebex is not connected.");
            return;
        }

        Object rawAmount = amountExpression.getSingle(event);
        if (!(rawAmount instanceof Number amountNumber)) return;

        String note = null;
        if (noteExpression != null) {
            Object rawNote = noteExpression.getSingle(event);
            if (rawNote != null) note = rawNote.toString();
        }

        try {
            GiftCard giftCard = service.pluginApi().createGiftCard(amountNumber.doubleValue(), note, null).join();
            target.change(event, new Object[]{giftCard}, ChangeMode.SET);
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            SkriptTebex.getInstance().logger().warn("Failed to create a Tebex gift card: {}", cause.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "create a tebex gift card";
    }
}
