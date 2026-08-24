package com.github.cjh3139.skripttebex.registration;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.yggdrasil.Fields;
import com.github.cjh3139.skripttebex.api.model.*;
import com.github.cjh3139.skripttebex.api.model.Package;
import org.jetbrains.annotations.NotNull;

import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.Arrays;

public class Registration {
    static {
        Classes.registerClass(new ClassInfo<>(Package.class, "tebexpackage")
            .user("tebex ?packages?")
            .name("Tebex Package")
            .description("A package from your Tebex store listing.")
            .examples("send \"%name of {_package}%: %price of {_package}%\" to player")
            .parser(new Parser<Package>() {
                @Override
                public boolean canParse(@NotNull ParseContext context) {
                    return false;
                }

                @Override
                public @NotNull String toString(@NotNull Package pkg, int flags) {
                    String name = pkg.name();
                    return name != null ? name : "tebex package " + pkg.id();
                }

                @Override
                public @NotNull String toVariableNameString(@NotNull Package pkg) {
                    return "tebex package " + pkg.id();
                }
            })
            .serializer(new Serializer<Package>() {
                @Override
                public Fields serialize(Package pkg) throws NotSerializableException {
                    Fields fields = new Fields();
                    fields.putPrimitive("id", pkg.id());
                    fields.putObject("name", pkg.name());
                    fields.putObject("description", pkg.description());
                    fields.putObject("image", pkg.image());
                    fields.putObject("type", pkg.type());
                    fields.putPrimitive("basePrice", pkg.basePrice());
                    fields.putPrimitive("totalPrice", pkg.totalPrice());
                    fields.putObject("currency", pkg.currency());
                    fields.putPrimitive("discount", pkg.discount());
                    fields.putObject("slug", pkg.slug());
                    fields.putPrimitive("order", pkg.order());
                    return fields;
                }

                @Override
                public void deserialize(Package pkg, Fields fields) {
                    assert false;
                }

                @Override
                protected Package deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                    int id = fields.getPrimitive("id", int.class);
                    String name = fields.getObject("name", String.class);
                    String description = fields.getObject("description", String.class);
                    String image = fields.getObject("image", String.class);
                    String type = fields.getObject("type", String.class);
                    double basePrice = fields.getPrimitive("basePrice", double.class);
                    double totalPrice = fields.getPrimitive("totalPrice", double.class);
                    String currency = fields.getObject("currency", String.class);
                    double discount = fields.getPrimitive("discount", double.class);
                    String slug = fields.getObject("slug", String.class);
                    int order = fields.getPrimitive("order", int.class);
                    return new Package(id, name, description, image, type, basePrice, totalPrice, currency, discount, slug, order);
                }

                @Override
                public boolean mustSyncDeserialization() {
                    return false;
                }

                @Override
                protected boolean canBeInstantiated() {
                    return false;
                }
            }));

        Classes.registerClass(new ClassInfo<>(QueuedCommand.class, "tebexcommand")
            .user("tebex ?commands?")
            .name("Tebex Command")
            .description("A command queued by Tebex for delivery to a player.")
            .examples("on tebex command:\n\tbroadcast the tebex command")
            .parser(new Parser<QueuedCommand>() {
                @Override
                public boolean canParse(@NotNull ParseContext context) {
                    return false;
                }

                @Override
                public @NotNull String toString(@NotNull QueuedCommand command, int flags) {
                    String cmd = command.command();
                    return cmd != null ? cmd : "tebex command " + command.id();
                }

                @Override
                public @NotNull String toVariableNameString(@NotNull QueuedCommand command) {
                    return "tebex command " + command.id();
                }
            }));

        Classes.registerClass(new ClassInfo<>(Category.class, "tebexcategory")
            .user("tebex ?categor(y|ies)")
            .name("Tebex Category")
            .description("A category of packages from your Tebex store listing.")
            .examples("send \"%name of {_category}%\" to player")
            .parser(new Parser<Category>() {
                @Override
                public boolean canParse(@NotNull ParseContext context) {
                    return false;
                }

                @Override
                public @NotNull String toString(@NotNull Category category, int flags) {
                    String name = category.name();
                    return name != null ? name : "tebex category " + category.id();
                }

                @Override
                public @NotNull String toVariableNameString(@NotNull Category category) {
                    return "tebex category " + category.id();
                }
            })
            .serializer(new Serializer<Category>() {
                @Override
                public Fields serialize(Category category) throws NotSerializableException {
                    Fields fields = new Fields();
                    fields.putPrimitive("id", category.id());
                    fields.putObject("name", category.name());
                    fields.putObject("slug", category.slug());
                    fields.putObject("description", category.description());
                    fields.putPrimitive("order", category.order());
                    fields.putObject("displayType", category.displayType());
                    fields.putObject("imageUrl", category.imageUrl());
                    fields.putObject("packages", category.packagesOrEmpty().toArray(new Package[0]));
                    return fields;
                }

                @Override
                public void deserialize(Category category, Fields fields) {
                    assert false;
                }

                @Override
                protected Category deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                    int id = fields.getPrimitive("id", int.class);
                    String name = fields.getObject("name", String.class);
                    String slug = fields.getObject("slug", String.class);
                    String description = fields.getObject("description", String.class);
                    int order = fields.getPrimitive("order", int.class);
                    String displayType = fields.getObject("displayType", String.class);
                    String imageUrl = fields.getObject("imageUrl", String.class);
                    Package[] packages = fields.getObject("packages", Package[].class);
                    return new Category(id, name, slug, description, order, displayType, imageUrl,
                        packages == null ? java.util.List.of() : Arrays.asList(packages));
                }

                @Override
                public boolean mustSyncDeserialization() {
                    return false;
                }

                @Override
                protected boolean canBeInstantiated() {
                    return false;
                }
            }));

        Classes.registerClass(new ClassInfo<>(QueuedPlayer.class, "tebexplayer")
            .user("tebex ?players?")
            .name("Tebex Player")
            .description("A player with commands queued for delivery.")
            .examples("on tebex command:\n\tbroadcast name of player of the tebex command")
            .parser(new Parser<QueuedPlayer>() {
                @Override
                public boolean canParse(@NotNull ParseContext context) {
                    return false;
                }

                @Override
                public @NotNull String toString(@NotNull QueuedPlayer player, int flags) {
                    String name = player.name();
                    return name != null ? name : "tebex player " + player.id();
                }

                @Override
                public @NotNull String toVariableNameString(@NotNull QueuedPlayer player) {
                    return "tebex player " + player.id();
                }
            }));

        Classes.registerClass(new ClassInfo<>(Payment.class, "tebexpayment")
            .user("tebex ?payments?")
            .name("Tebex Payment")
            .description("A payment made in your Tebex store.")
            .examples("send \"%amount of {_payment}%\" to player")
            .parser(new Parser<Payment>() {
                @Override
                public boolean canParse(@NotNull ParseContext context) {
                    return false;
                }

                @Override
                public @NotNull String toString(@NotNull Payment payment, int flags) {
                    String txnId = payment.transactionId();
                    return txnId != null ? txnId : "tebex payment";
                }

                @Override
                public @NotNull String toVariableNameString(@NotNull Payment payment) {
                    String txnId = payment.transactionId();
                    return txnId != null ? "tebex payment " + txnId : "tebex payment";
                }
            }));

        Classes.registerClass(new ClassInfo<>(PlayerLookup.class, "tebexlookup")
            .user("tebex ?lookups?")
            .name("Tebex Player Lookup")
            .description("The result of looking up a player's Tebex purchase history.")
            .examples("set {_lookup} to tebex lookup of player")
            .parser(new Parser<PlayerLookup>() {
                @Override
                public boolean canParse(@NotNull ParseContext context) {
                    return false;
                }

                @Override
                public @NotNull String toString(@NotNull PlayerLookup lookup, int flags) {
                    PlayerLookup.Player player = lookup.player();
                    if (player == null) return "tebex lookup";
                    String username = player.username();
                    return username != null ? username : "tebex lookup " + player.id();
                }

                @Override
                public @NotNull String toVariableNameString(@NotNull PlayerLookup lookup) {
                    PlayerLookup.Player player = lookup.player();
                    return player != null ? "tebex lookup " + player.id() : "tebex lookup";
                }
            }));

        Classes.registerClass(new ClassInfo<>(Ban.class, "tebexban")
            .user("tebex ?bans?")
            .name("Tebex Ban")
            .description("A ban recorded in your Tebex store.")
            .examples("send \"%reason of {_ban}%\" to player")
            .parser(new Parser<Ban>() {
                @Override
                public boolean canParse(@NotNull ParseContext context) {
                    return false;
                }

                @Override
                public @NotNull String toString(@NotNull Ban ban, int flags) {
                    String reason = ban.reason();
                    return reason != null ? reason : "tebex ban " + ban.id();
                }

                @Override
                public @NotNull String toVariableNameString(@NotNull Ban ban) {
                    return "tebex ban " + ban.id();
                }
            }));

        Classes.registerClass(new ClassInfo<>(Coupon.class, "tebexcoupon")
            .user("tebex ?coupons?")
            .name("Tebex Coupon")
            .description("A coupon from your Tebex store.")
            .examples("send \"%code of {_coupon}%\" to player")
            .parser(new Parser<Coupon>() {
                @Override
                public boolean canParse(@NotNull ParseContext context) {
                    return false;
                }

                @Override
                public @NotNull String toString(@NotNull Coupon coupon, int flags) {
                    String code = coupon.code();
                    return code != null ? code : "tebex coupon " + coupon.id();
                }

                @Override
                public @NotNull String toVariableNameString(@NotNull Coupon coupon) {
                    return "tebex coupon " + coupon.id();
                }
            })
            .serializer(new Serializer<Coupon>() {
                @Override
                public Fields serialize(Coupon coupon) throws NotSerializableException {
                    Fields fields = new Fields();
                    fields.putPrimitive("id", coupon.id());
                    fields.putObject("code", coupon.code());
                    fields.putObject("effectiveOn", coupon.effectiveOn());
                    Coupon.Discount discount = coupon.discount();
                    fields.putObject("discountType", discount == null ? null : discount.type());
                    fields.putPrimitive("discountPercentage", discount == null ? 0d : discount.percentage());
                    fields.putPrimitive("discountValue", discount == null ? 0d : discount.value());
                    return fields;
                }

                @Override
                public void deserialize(Coupon coupon, Fields fields) {
                    assert false;
                }

                @Override
                protected Coupon deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                    int id = fields.getPrimitive("id", int.class);
                    String code = fields.getObject("code", String.class);
                    String effectiveOn = fields.getObject("effectiveOn", String.class);
                    String discountType = fields.getObject("discountType", String.class);
                    double discountPercentage = fields.getPrimitive("discountPercentage", double.class);
                    double discountValue = fields.getPrimitive("discountValue", double.class);
                    Coupon.Discount discount = discountType == null
                        ? null
                        : new Coupon.Discount(discountType, discountPercentage, discountValue);
                    return new Coupon(id, code, effectiveOn, discount);
                }

                @Override
                public boolean mustSyncDeserialization() {
                    return false;
                }

                @Override
                protected boolean canBeInstantiated() {
                    return false;
                }
            }));

        Classes.registerClass(new ClassInfo<>(GiftCard.class, "tebexgiftcard")
            .user("tebex ?gift ?cards?")
            .name("Tebex Gift Card")
            .description("A gift card from your Tebex store.")
            .examples("send \"%balance of {_giftcard}%\" to player")
            .parser(new Parser<GiftCard>() {
                @Override
                public boolean canParse(@NotNull ParseContext context) {
                    return false;
                }

                @Override
                public @NotNull String toString(@NotNull GiftCard giftCard, int flags) {
                    String cardNumber = giftCard.cardNumber();
                    return cardNumber != null ? cardNumber : "tebex gift card " + giftCard.id();
                }

                @Override
                public @NotNull String toVariableNameString(@NotNull GiftCard giftCard) {
                    return "tebex gift card " + giftCard.id();
                }
            })
            .serializer(new Serializer<GiftCard>() {
                @Override
                public Fields serialize(GiftCard giftCard) throws NotSerializableException {
                    Fields fields = new Fields();
                    fields.putPrimitive("id", giftCard.id());
                    fields.putObject("cardNumber", giftCard.cardNumber());
                    fields.putPrimitive("balance", giftCard.balance());
                    fields.putObject("note", giftCard.note());
                    fields.putObject("expiresAt", giftCard.expiresAt());
                    fields.putPrimitive("voided", giftCard.voided());
                    return fields;
                }

                @Override
                public void deserialize(GiftCard giftCard, Fields fields) {
                    assert false;
                }

                @Override
                protected GiftCard deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                    int id = fields.getPrimitive("id", int.class);
                    String cardNumber = fields.getObject("cardNumber", String.class);
                    double balance = fields.getPrimitive("balance", double.class);
                    String note = fields.getObject("note", String.class);
                    String expiresAt = fields.getObject("expiresAt", String.class);
                    boolean voided = fields.getPrimitive("voided", boolean.class);
                    return new GiftCard(id, cardNumber, balance, note, expiresAt, voided);
                }

                @Override
                public boolean mustSyncDeserialization() {
                    return false;
                }

                @Override
                protected boolean canBeInstantiated() {
                    return false;
                }
            }));

        Classes.registerClass(new ClassInfo<>(Sale.class, "tebexsale")
            .user("tebex ?sales?")
            .name("Tebex Sale")
            .description("A sale running in your Tebex store.")
            .examples("send \"%name of {_sale}%\" to player")
            .parser(new Parser<Sale>() {
                @Override
                public boolean canParse(@NotNull ParseContext context) {
                    return false;
                }

                @Override
                public @NotNull String toString(@NotNull Sale sale, int flags) {
                    String name = sale.name();
                    return name != null ? name : "tebex sale " + sale.id();
                }

                @Override
                public @NotNull String toVariableNameString(@NotNull Sale sale) {
                    return "tebex sale " + sale.id();
                }
            }));

        Classes.registerClass(new ClassInfo<>(CommunityGoal.class, "tebexgoal")
            .user("tebex ?(goal|community goal)s?")
            .name("Tebex Community Goal")
            .description("A community goal from your Tebex store.")
            .examples("send \"%name of {_goal}%: %progress of {_goal}%\" to player")
            .parser(new Parser<CommunityGoal>() {
                @Override
                public boolean canParse(@NotNull ParseContext context) {
                    return false;
                }

                @Override
                public @NotNull String toString(@NotNull CommunityGoal goal, int flags) {
                    String name = goal.name();
                    return name != null ? name : "tebex goal " + goal.id();
                }

                @Override
                public @NotNull String toVariableNameString(@NotNull CommunityGoal goal) {
                    return "tebex goal " + goal.id();
                }
            }));

        Classes.registerClass(new ClassInfo<>(ServerInformation.class, "tebexstore")
            .user("tebex ?stores?")
            .name("Tebex Store")
            .description("Information about your Tebex store.")
            .examples("send \"%name of tebex store%\" to player")
            .parser(new Parser<ServerInformation>() {
                @Override
                public boolean canParse(@NotNull ParseContext context) {
                    return false;
                }

                @Override
                public @NotNull String toString(@NotNull ServerInformation info, int flags) {
                    ServerInformation.Account account = info.account();
                    if (account == null) return "tebex store";
                    String name = account.name();
                    return name != null ? name : "tebex store " + account.id();
                }

                @Override
                public @NotNull String toVariableNameString(@NotNull ServerInformation info) {
                    ServerInformation.Account account = info.account();
                    return account != null ? "tebex store " + account.id() : "tebex store";
                }
            }));
    }
}
