package com.github.cjh3139.skripttebex.elements.expressions;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.github.cjh3139.skripttebex.api.model.Coupon;
import com.github.cjh3139.skripttebex.api.model.GiftCard;
import org.jetbrains.annotations.Nullable;

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
