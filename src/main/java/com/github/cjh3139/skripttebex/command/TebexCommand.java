package com.github.cjh3139.skripttebex.command;

import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.exception.TebexApiException;
import com.github.cjh3139.skripttebex.api.model.PlayerLookup;
import com.github.cjh3139.skripttebex.config.TebexConfig;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsLookup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class TebexCommand extends Command {
    public TebexCommand() {
        super("tebex");

        setDefaultExecutor((sender, context) -> {
            if (!allowed(sender, "help")) return;
            sendHelp(sender);
        });

        var helpLiteral = ArgumentType.Literal("help");
        addSyntax((sender, context) -> {
            if (!allowed(sender, "help")) return;
            sendHelp(sender);
        }, helpLiteral);

        var secretLiteral = ArgumentType.Literal("secret");
        var keyArgument = ArgumentType.String("key");
        addSyntax((sender, context) -> {
            if (!allowed(sender, "secret")) return;
            TebexService service = TebexService.get();
            if (service == null) {
                sender.sendMessage(Component.text("Tebex is not initialized.", NamedTextColor.RED));
                return;
            }

            String key = context.get(keyArgument);
            CompletableFuture.runAsync(() -> service.setSecretKey(key))
                .whenComplete((ignored, error) -> {
                    if (error != null) {
                        reportFailure(sender, "save the secret key", error);
                    } else {
                        sender.sendMessage(Component.text(
                            "Secret key saved, reconnecting to Tebex.", NamedTextColor.GREEN));
                    }
                });
        }, secretLiteral, keyArgument);

        var infoLiteral = ArgumentType.Literal("info");
        addSyntax((sender, context) -> {
            if (!allowed(sender, "info")) return;
            TebexService service = TebexService.get();
            if (service == null || !service.isConnected()) {
                sender.sendMessage(Component.text("Tebex is not connected.", NamedTextColor.RED));
                return;
            }
            service.cache().information().ifPresentOrElse(information -> {
                sender.sendMessage(Component.text("Store: " + information.account().name(), NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Domain: " + information.account().domain()));
                sender.sendMessage(Component.text("Server: " + information.server().name()));
                sender.sendMessage(Component.text("Currency: " + information.account().currency().iso4217()));
                sender.sendMessage(Component.text("Next check in "
                    + (service.queue() == null ? "-" : service.queue().nextCheckSeconds()) + "s"));
            }, () -> sender.sendMessage(Component.text("No Tebex store information cached yet.", NamedTextColor.YELLOW)));
        }, infoLiteral);

        var forcecheckLiteral = ArgumentType.Literal("forcecheck");
        addSyntax((sender, context) -> {
            if (!allowed(sender, "forcecheck")) return;
            TebexService service = TebexService.get();
            if (service == null || !service.isConnected() || service.queue() == null) {
                sender.sendMessage(Component.text("Tebex queue is not running.", NamedTextColor.RED));
                return;
            }
            sender.sendMessage(Component.text("Forcing a Tebex queue check...", NamedTextColor.GRAY));
            service.queue().checkNow().whenComplete((ignored, error) -> {
                if (error != null) {
                    reportFailure(sender, "run the Tebex queue check", error);
                } else {
                    sender.sendMessage(Component.text("Queue check complete.", NamedTextColor.GREEN));
                }
            });
        }, forcecheckLiteral);

        var refreshLiteral = ArgumentType.Literal("refresh");
        addSyntax((sender, context) -> {
            if (!allowed(sender, "refresh")) return;
            TebexService service = TebexService.get();
            if (service == null || !service.isConnected()) {
                sender.sendMessage(Component.text("Tebex is not connected.", NamedTextColor.RED));
                return;
            }
            service.refreshListing().whenComplete((ignored, error) -> {
                if (error != null) {
                    reportFailure(sender, "refresh the Tebex listing", error);
                } else {
                    sender.sendMessage(Component.text(
                        "Refreshed " + service.cache().categories().size() + " categories.", NamedTextColor.GREEN));
                }
            });
        }, refreshLiteral);

        var lookupLiteral = ArgumentType.Literal("lookup");
        var lookupPlayerArgument = ArgumentType.Word("player");
        addSyntax((sender, context) -> {
            if (!allowed(sender, "lookup")) return;
            TebexService service = TebexService.get();
            if (service == null || !service.isConnected() || service.pluginApi() == null) {
                sender.sendMessage(Component.text("Tebex is not connected.", NamedTextColor.RED));
                return;
            }
            String username = context.get(lookupPlayerArgument);
            service.pluginApi().lookup(username).whenComplete((lookup, error) -> {
                if (error != null) {
                    reportFailure(sender, "look up that player", error);
                    return;
                }

                PlayerLookup.Player player = lookup == null ? null : lookup.player();
                if (player == null) {
                    sender.sendMessage(Component.text(
                        "No Tebex player record found for " + username, NamedTextColor.YELLOW));
                } else {
                    sender.sendMessage(Component.text("Username: " + player.username(), NamedTextColor.GOLD));
                    sender.sendMessage(Component.text("Plugin username ID: " + player.pluginUsernameId()));
                }
                List<com.github.cjh3139.skripttebex.api.model.Payment> payments =
                    lookup == null || lookup.payments() == null ? List.of() : lookup.payments();
                double total = payments.stream()
                    .mapToDouble(com.github.cjh3139.skripttebex.api.model.Payment::amount)
                    .sum();
                sender.sendMessage(Component.text("Payments: " + payments.size() + " totaling " + total));
            });
        }, lookupLiteral, lookupPlayerArgument);

        var banLiteral = ArgumentType.Literal("ban");
        var banPlayerArgument = ArgumentType.Word("player");
        Argument<String> banReasonArgument = ArgumentType.String("reason").setDefaultValue("");
        addSyntax((sender, context) -> {
            if (!allowed(sender, "ban")) return;
            TebexService service = TebexService.get();
            if (service == null || !service.isConnected() || service.pluginApi() == null) {
                sender.sendMessage(Component.text("Tebex is not connected.", NamedTextColor.RED));
                return;
            }
            String username = context.get(banPlayerArgument);
            String reason = context.get(banReasonArgument);
            service.pluginApi().createBan(username, reason, null).whenComplete((ban, error) -> {
                if (error != null) {
                    reportFailure(sender, "create the Tebex ban", error);
                } else {
                    sender.sendMessage(Component.text("Banned " + username + " on Tebex.", NamedTextColor.GREEN));
                }
            });
        }, banLiteral, banPlayerArgument, banReasonArgument);

        var sendlinkLiteral = ArgumentType.Literal("sendlink");
        var sendlinkPlayerArgument = ArgumentType.Word("player");
        var sendlinkPackageArgument = ArgumentType.Integer("packageId");
        addSyntax((sender, context) -> {
            if (!allowed(sender, "sendlink")) return;
            TebexService service = TebexService.get();
            if (service == null || !service.isConnected() || service.pluginApi() == null) {
                sender.sendMessage(Component.text("Tebex is not connected.", NamedTextColor.RED));
                return;
            }
            String username = context.get(sendlinkPlayerArgument);
            int packageId = context.get(sendlinkPackageArgument);
            service.pluginApi().checkout(packageId, username).whenComplete((checkout, error) -> {
                if (error != null) {
                    reportFailure(sender, "create the checkout link", error);
                    return;
                }
                Player target = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(username);
                if (target == null) {
                    sender.sendMessage(Component.text(
                        username + " is not online, link not delivered: " + checkout.url(), NamedTextColor.YELLOW));
                    return;
                }
                String url = checkout.url();
                target.sendMessage(Component.text("Click to complete your Tebex purchase: ", NamedTextColor.GOLD)
                    .append(Component.text(url, NamedTextColor.AQUA).clickEvent(ClickEvent.openUrl(url))));
                sender.sendMessage(Component.text("Sent a checkout link to " + username, NamedTextColor.GREEN));
            });
        }, sendlinkLiteral, sendlinkPlayerArgument, sendlinkPackageArgument);

        var reportLiteral = ArgumentType.Literal("report");
        var reportMessageArgument = ArgumentType.StringArray("message");
        addSyntax((sender, context) -> {
            if (!allowed(sender, "report")) return;
            TebexService service = TebexService.get();
            TebexConfig config = SkriptTebex.getInstance().config();
            String[] words = context.get(reportMessageArgument);
            String message = String.join(" ", words == null ? new String[0] : words);

            CompletableFuture.supplyAsync(() -> {
                String body = DiagnosticReport.render(config, service, message);
                File reportsFolder = new File(SkriptTebex.getInstance().dataFolder(), "reports");
                return DiagnosticReport.write(reportsFolder, body);
            }).whenComplete((written, error) -> {
                if (error != null) {
                    reportFailure(sender, "write the diagnostic report", error);
                } else {
                    sender.sendMessage(Component.text(
                        "Wrote diagnostic report to " + written.getAbsolutePath(), NamedTextColor.GREEN));
                }
            });
        }, reportLiteral, reportMessageArgument);

        var debugLiteral = ArgumentType.Literal("debug");
        addSyntax((sender, context) -> {
            if (!allowed(sender, "debug")) return;
            TebexConfig config = SkriptTebex.getInstance().config();
            boolean verbose = !config.verbose();

            CompletableFuture.runAsync(() -> config.setVerbose(verbose))
                .whenComplete((ignored, error) -> {
                    if (error != null) {
                        reportFailure(sender, "save the debug setting", error);
                    } else {
                        sender.sendMessage(Component.text(
                            "Tebex verbose logging is now " + (verbose ? "ON" : "OFF"), NamedTextColor.GREEN));
                    }
                });
        }, debugLiteral);
    }

    private static boolean allowed(CommandSender sender, String subcommand) {
        if (!LuckPermsLookup.hasPermission(sender, "tebex." + subcommand)) {
            sender.sendMessage(Component.text("You lack tebex." + subcommand, NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private static void reportFailure(CommandSender sender, String action, Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        String detail = redact(cause.getMessage() == null ? cause.toString() : cause.getMessage());
        SkriptTebex.getInstance().logger().warn("Failed to {}: {}", action, detail, cause);

        String reason = cause instanceof TebexApiException apiException
            ? "Tebex API returned HTTP " + apiException.statusCode()
            : cause.getClass().getSimpleName();
        sender.sendMessage(Component.text(
            "Failed to " + action + " (" + redact(reason) + ").", NamedTextColor.RED));
    }

    private static String redact(String text) {
        if (text == null) return null;
        TebexConfig config = SkriptTebex.getInstance().config();
        String secret = config == null ? "" : config.secretKey();
        return secret.isBlank() ? text : text.replace(secret, "<redacted>");
    }

    private static void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("/tebex help | secret <key> | info | forcecheck | refresh"
            + " | lookup <player> | ban <player> [reason] | sendlink <player> <packageId>"
            + " | report <message> | debug", NamedTextColor.GOLD));
    }
}
