package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.github.cjh3139.skripttebex.api.model.Coupon;
import com.github.cjh3139.skripttebex.api.model.GiftCard;
import org.jetbrains.annotations.Nullable;

@Name("Tebex Code")
@Description("The code of a Tebex coupon or gift card.")
@Examples("send \"%code of {_coupon}%\" to player")
@Since("1.0.0")
public class ExprTebexCode extends SimplePropertyExpression<Object, String> {
    static {
        register(ExprTebexCode.class, String.class, "[tebex] code", "tebexcoupons/tebexgiftcards");
    }

    @Override
    public @Nullable String convert(Object from) {
        return switch (from) {
            case Coupon coupon -> coupon.code();
            case GiftCard giftCard -> giftCard.cardNumber();
            default -> null;
        };
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return "tebex code";
    }
}
