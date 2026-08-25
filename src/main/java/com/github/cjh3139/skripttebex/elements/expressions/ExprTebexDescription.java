package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.github.cjh3139.skripttebex.api.model.Category;
import com.github.cjh3139.skripttebex.api.model.CommunityGoal;
import com.github.cjh3139.skripttebex.api.model.Package;
import org.jetbrains.annotations.Nullable;

@Name("Tebex Description")
@Description("The description of a Tebex package, category or community goal.")
@Examples("send \"%description of {_package}%\" to player")
@Since("1.0.0")
public class ExprTebexDescription extends SimplePropertyExpression<Object, String> {
    static {
        register(ExprTebexDescription.class, String.class, "[tebex] description",
            "tebexpackages/tebexcategories/tebexgoals");
    }

    @Override
    public @Nullable String convert(Object from) {
        return switch (from) {
            case Package pkg -> pkg.description();
            case Category category -> category.description();
            case CommunityGoal goal -> goal.description();
            default -> null;
        };
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return "tebex description";
    }
}
