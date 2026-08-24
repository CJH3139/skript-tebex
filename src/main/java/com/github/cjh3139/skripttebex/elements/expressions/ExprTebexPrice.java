package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.github.cjh3139.skripttebex.api.model.Package;
import org.jetbrains.annotations.Nullable;

public class ExprTebexPrice extends SimplePropertyExpression<Package, Number> {
    static {
        register(ExprTebexPrice.class, Number.class, "[tebex] price", "tebexpackages");
    }

    @Override
    public @Nullable Number convert(Package pkg) {
        return pkg.totalPrice();
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "tebex price";
    }
}
