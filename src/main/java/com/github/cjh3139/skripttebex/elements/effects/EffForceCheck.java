package com.github.cjh3139.skripttebex.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.AsyncEffect;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.queue.CommandQueue;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletionException;

public class EffForceCheck extends AsyncEffect {
    static {
        Skript.registerEffect(EffForceCheck.class, "force [a] tebex queue check");
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        getParser().setHasDelayBefore(Kleenean.TRUE);
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null || !service.isConnected()) {
            SkriptTebex.getInstance().logger().warn("Cannot force a tebex queue check: Tebex is not connected.");
            return;
        }

        CommandQueue queue = service.queue();
        if (queue == null) {
            SkriptTebex.getInstance().logger().warn("Cannot force a tebex queue check: the queue is not running.");
            return;
        }

        try {
            queue.checkNow().join();
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            SkriptTebex.getInstance().logger().warn("Failed to force a Tebex queue check: {}", cause.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "force a tebex queue check";
    }
}
