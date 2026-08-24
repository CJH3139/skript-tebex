package com.github.cjh3139.skripttebex.queue;

import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.Package;
import com.github.cjh3139.skripttebex.api.model.QueuedCommand;
import com.github.cjh3139.skripttebex.api.model.QueuedPlayer;
import com.github.cjh3139.skripttebex.elements.events.TebexCommandEvent;
import com.github.cjh3139.skripttebex.elements.events.TebexPurchaseEvent;
import com.github.cjh3139.skripttebex.elements.events.TebexQueueCheckEvent;
import com.github.cjh3139.skripttebex.elements.events.TebexQueueErrorEvent;
import com.github.cjh3139.skripttebex.util.TebexEventDispatcher;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.thread.TickThread;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MinestomQueuePlatform implements QueuePlatform {
    private static final Duration DISPATCH_TIMEOUT = Duration.ofSeconds(30);

    private final ErrorReporter errorReporter = new ErrorReporter(
        (message, cause) -> {
            if (cause != null) {
                SkriptTebex.getInstance().logger().warn(message, cause);
            } else {
                SkriptTebex.getInstance().logger().warn(message);
            }
        });

    @Override
    public boolean isOnline(String usernameId) {
        return findPlayer(usernameId).isPresent();
    }

    @Override
    public int freeSlots(String usernameId) {
        return findPlayer(usernameId)
            .map(player -> {
                int free = 0;
                for (int slot = 0; slot < 36; slot++) {
                    if (player.getInventory().getItemStack(slot).isAir()) free++;
                }
                return free;
            })
            .orElse(0);
    }

    @Override
    public DeliveryResult dispatch(QueuedCommand command, QueuedPlayer player) {
        if (Thread.currentThread() instanceof TickThread) {
            try {
                return fireAndExecute(command, player);
            } catch (RuntimeException runtimeException) {
                throw runtimeException;
            } catch (Throwable failure) {
                throw new RuntimeException("Failed to run command " + command.id()
                    + " on the server thread", failure);
            }
        }

        CompletableFuture<DeliveryResult> executed = new CompletableFuture<>();
        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            try {
                executed.complete(fireAndExecute(command, player));
            } catch (Throwable t) {
                executed.completeExceptionally(t);
            }
        });

        try {
            return executed.get(DISPATCH_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException timeout) {
            errorReporter.report("dispatch-timeout:" + command.id(),
                "Command " + command.id() + " did not finish running on the server thread within "
                    + DISPATCH_TIMEOUT.toSeconds() + "s; it is still scheduled and will run once the "
                    + "server catches up, so it is being treated as delivered to avoid Tebex "
                    + "redelivering it and running it twice",
                timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for command " + command.id()
                + " to run on the server thread", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            throw new RuntimeException("Failed to run command " + command.id() + " on the server thread", cause);
        }
        return DeliveryResult.EXECUTED;
    }

    private DeliveryResult fireAndExecute(QueuedCommand command, QueuedPlayer player) {
        Package pkg = TebexService.get().cache().packageById(command.packageId()).orElse(null);
        TebexCommandEvent event = new TebexCommandEvent(command, player, pkg);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return event.isMarkExecuted() ? DeliveryResult.CANCELLED_MARK_EXECUTED : DeliveryResult.CANCELLED_REDELIVER;
        }
        MinecraftServer.getCommandManager().executeServerCommand(command.command());
        return DeliveryResult.EXECUTED;
    }

    @Override
    public void onPurchase(int paymentId, List<QueuedCommand> commands) {
        SkriptTebex.getInstance().logger().info("Tebex payment {} delivered {} command(s)",
            paymentId, commands.size());
        QueuedCommand first = commands.getFirst();
        Package pkg = TebexService.get().cache().packageById(first.packageId()).orElse(null);

        TebexEventDispatcher.fireOnTickThread(
            new TebexPurchaseEvent(paymentId, first.player(), pkg, commands));
    }

    @Override
    public void onCheck(int duePlayerCount) {
        if (SkriptTebex.getInstance().config().verbose()) {
            SkriptTebex.getInstance().logger().info("Tebex queue check: {} due player(s)", duePlayerCount);
        }

        errorReporter.clearAll();

        TebexEventDispatcher.fireOnTickThread(new TebexQueueCheckEvent(duePlayerCount));
    }

    @Override
    public void onError(String reason, Throwable cause) {
        errorReporter.report(reason, "Tebex queue error: " + reason, cause);

        TebexEventDispatcher.fireOnTickThread(new TebexQueueErrorEvent(reason));
    }

    protected Optional<Player> findPlayer(String usernameId) {
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
            .filter(player -> player.getUuid().toString().replace("-", "").equalsIgnoreCase(usernameId)
                || player.getUsername().equalsIgnoreCase(usernameId))
            .findFirst();
    }
}
