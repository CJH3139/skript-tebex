package com.github.cjh3139.skripttebex.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.util.AsyncEffect;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.Coupon;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;

@Name("Create Tebex Coupon")
@Description(
    "Creates a percentage discount coupon in your Tebex store and stores it in a variable. The given name is used " +
    "as the coupon code; a random code is generated when no name is given. The coupon never expires unless an " +
    "expiry timespan is given. This effect waits for the Tebex API and therefore delays the script."
)
@Examples("create a tebex coupon named \"SUMMER\" with 25 percent discount expiring in 7 days and store it in {_coupon}\nsend \"Use %code of {_coupon}% at checkout\" to player")
@Since("1.0.0")
public class EffCreateCoupon extends AsyncEffect {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    static {
        Skript.registerEffect(EffCreateCoupon.class,
            "create [a] tebex coupon [named %-string%] with %number% [percent] discount " +
                "[expiring in %-timespan%] and store (it|the result) in %~object%");
    }

    @Nullable
    private Expression<?> nameExpression;
    private Expression<?> discountExpression;
    @Nullable
    private Expression<?> expiryExpression;
    private Variable<?> target;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        getParser().setHasDelayBefore(Kleenean.TRUE);
        nameExpression = expressions[0];
        discountExpression = expressions[1];
        expiryExpression = expressions[2];
        if (!(expressions[3] instanceof Variable<?> variable)) {
            Skript.error("The result of a tebex coupon must be stored in a variable.");
            return false;
        }
        target = variable;
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null || !service.isConnected() || service.pluginApi() == null) {
            SkriptTebex.getInstance().logger().warn("Cannot create a tebex coupon: Tebex is not connected.");
            return;
        }

        Object rawDiscount = discountExpression.getSingle(event);
        if (!(rawDiscount instanceof Number discountNumber)) return;

        String code = null;
        if (nameExpression != null) {
            Object rawName = nameExpression.getSingle(event);
            if (rawName != null) code = rawName.toString();
        }
        if (code == null || code.isBlank()) code = randomCode();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", code);
        request.put("effective_on", "cart");
        request.put("discount_type", "percentage");
        request.put("discount_percentage", discountNumber.doubleValue());
        request.put("expire_type", "timestamp");
        if (expiryExpression != null) {
            Object rawExpiry = expiryExpression.getSingle(event);
            if (rawExpiry instanceof Timespan timespan) {
                long millis = timespan.getAs(Timespan.TimePeriod.MILLISECOND);
                LocalDate expireDate = Instant.now().plusMillis(millis).atZone(ZoneId.systemDefault()).toLocalDate();
                request.put("expire_date", expireDate.format(DATE_FORMAT));
            }
        }
        request.put("basket_type", "single");
        request.put("start_date", LocalDate.now().format(DATE_FORMAT));
        request.put("user_limit", 0);
        request.put("discount_application_method", 0);

        try {
            Coupon coupon = service.pluginApi().createCoupon(request).join();
            target.change(event, new Object[]{coupon}, ChangeMode.SET);
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            SkriptTebex.getInstance().logger().warn("Failed to create a Tebex coupon: {}", cause.getMessage());
        }
    }

    private static String randomCode() {
        StringBuilder code = new StringBuilder("SK");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "create a tebex coupon";
    }
}
