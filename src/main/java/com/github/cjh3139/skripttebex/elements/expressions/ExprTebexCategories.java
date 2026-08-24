package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.Category;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprTebexCategories extends SimpleExpression<Category> {
    static {
        Skript.registerExpression(ExprTebexCategories.class, Category.class, ExpressionType.SIMPLE,
            "[the] tebex categories");
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        return true;
    }

    @Override
    protected Category @Nullable [] get(Event event) {
        TebexService service = TebexService.get();
        if (service == null) return new Category[0];
        return service.cache().categories().toArray(Category[]::new);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Category> getReturnType() {
        return Category.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "tebex categories";
    }
}
