package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.github.cjh3139.skripttebex.api.model.Category;
import com.github.cjh3139.skripttebex.api.model.CommunityGoal;
import com.github.cjh3139.skripttebex.api.model.Package;
import com.github.cjh3139.skripttebex.api.model.QueuedPlayer;
import com.github.cjh3139.skripttebex.api.model.Sale;
import org.jetbrains.annotations.Nullable;

public class ExprTebexName extends SimplePropertyExpression<Object, String> {
    static {
        register(ExprTebexName.class, String.class, "[tebex] name",
            "tebexpackages/tebexcategories/tebexplayers/tebexsales/tebexgoals");
    }

    @Override
    public @Nullable String convert(Object from) {
        return switch (from) {
            case Package pkg -> pkg.name();
            case Category category -> category.name();
            case QueuedPlayer player -> player.name();
            case Sale sale -> sale.name();
            case CommunityGoal goal -> goal.name();
            default -> null;
        };
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return "tebex name";
    }
}
