package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.github.cjh3139.skripttebex.api.model.Ban;
import com.github.cjh3139.skripttebex.api.model.Category;
import com.github.cjh3139.skripttebex.api.model.CommunityGoal;
import com.github.cjh3139.skripttebex.api.model.Coupon;
import com.github.cjh3139.skripttebex.api.model.GiftCard;
import com.github.cjh3139.skripttebex.api.model.Package;
import org.jetbrains.annotations.Nullable;

@Name("Tebex ID")
@Description("The id of a Tebex package, category, coupon, gift card, ban or community goal.")
@Examples("send \"%id of {_package}%\" to player")
@Since("1.0.0")
public class ExprTebexId extends SimplePropertyExpression<Object, Number> {
    static {
        register(ExprTebexId.class, Number.class, "[tebex] id",
            "tebexpackages/tebexcategories/tebexcoupons/tebexgiftcards/tebexbans/tebexgoals");
    }

    @Override
    public @Nullable Number convert(Object from) {
        return switch (from) {
            case Package pkg -> pkg.id();
            case Category category -> category.id();
            case Coupon coupon -> coupon.id();
            case GiftCard giftCard -> giftCard.id();
            case Ban ban -> ban.id();
            case CommunityGoal goal -> goal.id();
            default -> null;
        };
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "tebex id";
    }
}
