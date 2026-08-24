package com.github.cjh3139.skripttebex.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.AsyncEffect;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.TebexService;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletionException;

public class EffTebexBan extends AsyncEffect {
    static {
        Skript.registerEffect(EffTebexBan.class,
            "tebex ban %player/string% [for %-string%] [with ip %-string%]");
    }

    private Expression<?> playerExpression;
    @Nullable
    private Expression<?> reasonExpression;
    @Nullable
    private Expression<?> ipExpression;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        getParser().setHasDelayBefore(Kleenean.TRUE);
        playerExpression = expressions[0];
        reasonExpression = expressions[1];
        ipExpression = expressions[2];
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null || !service.isConnected() || service.pluginApi() == null) {
            SkriptTebex.getInstance().logger().warn("Cannot create a tebex ban: Tebex is not connected.");
            return;
        }

        Object rawPlayer = playerExpression.getSingle(event);
        if (rawPlayer == null) return;
        String user = rawPlayer instanceof Player player ? player.getUsername() : rawPlayer.toString();

        String reason = null;
        if (reasonExpression != null) {
            Object rawReason = reasonExpression.getSingle(event);
            if (rawReason != null) reason = rawReason.toString();
        }

        String ip = null;
        if (ipExpression != null) {
            Object rawIp = ipExpression.getSingle(event);
            if (rawIp != null) ip = rawIp.toString();
        }

        try {
            service.pluginApi().createBan(user, reason, ip).join();
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            SkriptTebex.getInstance().logger().warn("Failed to create a Tebex ban: {}", cause.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "tebex ban " + playerExpression.toString(event, debug);
    }
}
