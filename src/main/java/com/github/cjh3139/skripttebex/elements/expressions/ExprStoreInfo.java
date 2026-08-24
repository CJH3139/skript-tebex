package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.ServerInformation;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ExprStoreInfo extends SimpleExpression<Object> {
    static {
        Skript.registerExpression(ExprStoreInfo.class, Object.class, ExpressionType.SIMPLE,
            "[the] tebex store (0:name|1:id|2:domain|3:currency)");
    }

    private int mark;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        mark = result.mark;
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        TebexService service = TebexService.get();
        if (service == null) return new Object[0];
        Optional<ServerInformation> information = service.cache().information();
        if (information.isEmpty()) return new Object[0];

        ServerInformation.Account account = information.get().account();
        if (account == null) return new Object[0];

        Object value = switch (mark) {
            case 1 -> account.id();
            case 2 -> account.domain();
            case 3 -> account.currency() == null ? null : account.currency().iso4217();
            default -> account.name();
        };
        if (value == null) return new Object[0];
        return new Object[]{value};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return mark == 1 ? Number.class : String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return switch (mark) {
            case 1 -> "tebex store id";
            case 2 -> "tebex store domain";
            case 3 -> "tebex store currency";
            default -> "tebex store name";
        };
    }
}
