package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.github.cjh3139.skripttebex.api.model.Package;
import org.jetbrains.annotations.Nullable;

@Name("Tebex Price")
@Description("The total price of a Tebex package, including any discount applied by a sale.")
@Examples("send \"%price of {_package}%\" to player")
@Since("1.0.0")
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
