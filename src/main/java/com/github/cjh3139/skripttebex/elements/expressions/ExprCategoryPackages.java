package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.api.model.Category;
import com.github.cjh3139.skripttebex.api.model.Package;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ExprCategoryPackages extends PropertyExpression<Category, Package> {
    static {
        register(ExprCategoryPackages.class, Package.class, "packages", "tebexcategories");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        setExpr((Expression<? extends Category>) expressions[0]);
        return true;
    }

    @Override
    protected Package[] get(Event event, Category[] source) {
        List<Package> packages = new ArrayList<>();
        for (Category category : source) {
            packages.addAll(category.packagesOrEmpty());
        }
        return packages.toArray(new Package[0]);
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
        return "packages of " + getExpr().toString(event, debug);
    }
}
