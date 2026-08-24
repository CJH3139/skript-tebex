package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.Package;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprTebexPackages extends SimpleExpression<Package> {
    static {
        Skript.registerExpression(ExprTebexPackages.class, Package.class, ExpressionType.SIMPLE,
            "[the] tebex packages");
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        return true;
    }

    @Override
    protected Package @Nullable [] get(Event event) {
        TebexService service = TebexService.get();
        if (service == null) return new Package[0];
        return service.cache().packages().toArray(Package[]::new);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Package> getReturnType() {
        return Package.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "tebex packages";
    }
}
