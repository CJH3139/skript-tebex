package com.github.cjh3139.skripttebex.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.TebexService;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class EffSetSecretKey extends Effect {
    static {
        Skript.registerEffect(EffSetSecretKey.class, "set [the] tebex secret key to %string%");
    }

    private Expression<String> keyExpr;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        keyExpr = (Expression<String>) expressions[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null) {
            SkriptTebex.getInstance().logger().warn("Cannot set the tebex secret key: Tebex is not initialised.");
            return;
        }

        String key = keyExpr.getSingle(event);
        if (key == null) return;

        CompletableFuture.runAsync(() -> service.setSecretKey(key))
            .exceptionally(error -> {
                Throwable cause = error instanceof CompletionException && error.getCause() != null
                    ? error.getCause() : error;
                SkriptTebex.getInstance().logger()
                    .warn("Failed to save the tebex secret key: {}", cause.getMessage());
                return null;
            });
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "set the tebex secret key to " + keyExpr.toString(event, debug);
    }
}
