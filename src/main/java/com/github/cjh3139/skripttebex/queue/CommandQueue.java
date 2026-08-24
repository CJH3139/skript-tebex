package com.github.cjh3139.skripttebex.queue;

import com.github.cjh3139.skripttebex.api.TebexPluginApi;
import com.github.cjh3139.skripttebex.api.model.CommandConditions;
import com.github.cjh3139.skripttebex.api.model.QueuedCommand;
import com.github.cjh3139.skripttebex.api.model.QueuedPlayer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

public final class CommandQueue {
    private static final int DEFAULT_INTERVAL = 90;
    private static final int MAX_INTERVAL = 900;

    private final TebexPluginApi api;
    private final QueuePlatform platform;
    private final QueueScheduler scheduler;

    private final Map<String, QueuedPlayer> duePlayers = new ConcurrentHashMap<>();

    private final Map<String, ConcurrentHashMap<Integer, QueuedCommand>> deferred = new ConcurrentHashMap<>();

    private final Map<String, Set<Integer>> dispatchedByPlayer = new ConcurrentHashMap<>();

    private final Map<String, Set<Integer>> announcedPayments = new ConcurrentHashMap<>();

    private final Set<String> playersInFlight = ConcurrentHashMap.newKeySet();

    private final Set<Integer> commandsInFlight = ConcurrentHashMap.newKeySet();

    private final AtomicReference<CompletableFuture<Void>> inFlightCheck = new AtomicReference<>();

    private final LongSupplier nanoClock;

    private volatile CompletableFuture<Void> lastCheck;
    private volatile long lastPollStartedNanos;
    private volatile boolean everPolled;

    private volatile boolean running;
    private volatile int nextCheckSeconds = DEFAULT_INTERVAL;

    public CommandQueue(TebexPluginApi api, QueuePlatform platform, QueueScheduler scheduler) {
        this(api, platform, scheduler, System::nanoTime);
    }

    CommandQueue(TebexPluginApi api, QueuePlatform platform, QueueScheduler scheduler, LongSupplier nanoClock) {
        this.api = api;
        this.platform = platform;
        this.scheduler = scheduler;
        this.nanoClock = nanoClock;
    }

    public void start() {
        running = true;
        checkNow().whenComplete((ignored, error) -> reschedule());
    }

    public void stop() {
        running = false;
    }

    public int nextCheckSeconds() {
        return nextCheckSeconds;
    }

    public List<QueuedPlayer> duePlayers() {
        return List.copyOf(duePlayers.values());
    }

    public List<QueuedCommand> deferredFor(String usernameId) {
        ConcurrentHashMap<Integer, QueuedCommand> pending = deferred.get(usernameId);
        return pending == null ? List.of() : List.copyOf(pending.values());
    }

    public CompletableFuture<Void> checkNow() {
        if (!pollFloorElapsed()) {
            CompletableFuture<Void> current = inFlightCheck.get();
            if (current != null) return current;
            CompletableFuture<Void> previous = lastCheck;
            return previous != null ? previous : CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> mine = new CompletableFuture<>();
        CompletableFuture<Void> existing = inFlightCheck.compareAndExchange(null, mine);
        if (existing != null) {
            return existing;
        }
        lastPollStartedNanos = nanoClock.getAsLong();
        everPolled = true;
        lastCheck = mine;
        runCheck().whenComplete((result, error) -> {
            inFlightCheck.set(null);
            if (error != null) {
                mine.completeExceptionally(error);
            } else {
                mine.complete(result);
            }
        });
        return mine;
    }

    private boolean pollFloorElapsed() {
        if (!everPolled) return true;
        long elapsed = nanoClock.getAsLong() - lastPollStartedNanos;
        return elapsed >= Duration.ofSeconds(nextCheckSeconds).toNanos();
    }

    private CompletableFuture<Void> runCheck() {
        return api.duePlayers()
            .thenCompose(due -> {
                nextCheckSeconds = Math.max(1, due.meta().nextCheck());
                platform.onCheck(due.players() == null ? 0 : due.players().size());

                if (due.players() != null) {
                    for (QueuedPlayer player : due.players()) {
                        duePlayers.put(player.uuid(), player);
                    }
                }

                CompletableFuture<Void> offline = due.meta().executeOffline()
                    ? api.offlineCommands().thenCompose(commands -> deliver(commands, null))
                    : CompletableFuture.completedFuture(null);

                return offline.thenCompose(ignored -> drainOnlinePlayers());
            })
            .exceptionally(error -> {
                Throwable cause = error instanceof CompletionException ? error.getCause() : error;
                nextCheckSeconds = Math.min(MAX_INTERVAL, Math.max(DEFAULT_INTERVAL, nextCheckSeconds) * 2);
                platform.onError(cause.getMessage() == null ? cause.toString() : cause.getMessage(), cause);
                return null;
            });
    }

    private void reschedule() {
        if (!running) return;
        scheduler.schedule(
            () -> checkNow().whenComplete((ignored, error) -> reschedule()),
            Duration.ofSeconds(nextCheckSeconds));
    }

    public void onPlayerJoin(String usernameId) {
        QueuedPlayer player = duePlayers.get(usernameId);
        if (player == null) return;
        if (!playersInFlight.add(usernameId)) {
            return;
        }
        CompletableFuture<Void> work = CompletableFuture.completedFuture(null);
        try {
            ConcurrentHashMap<Integer, QueuedCommand> waiting = deferred.remove(usernameId);
            CompletableFuture<Void> replayed = waiting == null || waiting.isEmpty()
                ? CompletableFuture.completedFuture(null)
                : deliver(List.copyOf(waiting.values()), player);

            work = replayed.thenCompose(ignored -> fetchOnlineCommands(player));
        } finally {
            work.whenComplete((ignored, error) -> {
                playersInFlight.remove(usernameId);
                evictIfSettled(usernameId);
            });
        }
    }

    private CompletableFuture<Void> drainOnlinePlayers() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (QueuedPlayer player : List.copyOf(duePlayers.values())) {
            if (!platform.isOnline(player.uuid())) continue;
            futures.add(fetchAndDeliverOnline(player));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<Void> fetchAndDeliverOnline(QueuedPlayer player) {
        String usernameId = player.uuid();
        if (!playersInFlight.add(usernameId)) {
            return CompletableFuture.completedFuture(null);
        }
        return fetchOnlineCommands(player)
            .whenComplete((ignored, error) -> {
                playersInFlight.remove(usernameId);
                evictIfSettled(usernameId);
            });
    }

    private CompletableFuture<Void> fetchOnlineCommands(QueuedPlayer player) {
        return api.onlineCommands(player.id())
            .thenCompose(commands -> deliver(commands, player))
            .exceptionally(error -> {
                platform.onError("Failed to fetch online commands for " + player.uuid(), error);
                return null;
            });
    }

    boolean isCommandInFlight(int commandId) {
        return commandsInFlight.contains(commandId);
    }

    Set<Integer> unconfirmedDispatchesFor(String usernameId) {
        Set<Integer> dispatched = dispatchedByPlayer.get(usernameId);
        return dispatched == null ? Set.of() : Set.copyOf(dispatched);
    }

    int suppressionEntryCount() {
        return dispatchedByPlayer.size();
    }

    Set<Integer> announcedPaymentsFor(String usernameId) {
        Set<Integer> announced = announcedPayments.get(usernameId);
        return announced == null ? Set.of() : Set.copyOf(announced);
    }

    boolean isDue(String usernameId) {
        return duePlayers.containsKey(usernameId);
    }

    private void evictIfSettled(String usernameId) {
        ConcurrentHashMap<Integer, QueuedCommand> pending = deferred.get(usernameId);
        if (pending != null && !pending.isEmpty()) return;

        Set<Integer> unconfirmed = dispatchedByPlayer.computeIfPresent(
            usernameId, (key, dispatched) -> dispatched.isEmpty() ? null : dispatched);
        if (unconfirmed != null) return;

        duePlayers.remove(usernameId);
        deferred.remove(usernameId);

        announcedPayments.remove(usernameId);
    }

    private void confirmDeleted(Collection<Integer> ids) {
        for (String usernameId : List.copyOf(dispatchedByPlayer.keySet())) {
            boolean[] emptied = {false};
            dispatchedByPlayer.compute(usernameId, (key, dispatched) -> {
                if (dispatched == null) return null;
                dispatched.removeAll(ids);
                if (dispatched.isEmpty()) {
                    emptied[0] = true;
                    return null;
                }
                return dispatched;
            });
            if (emptied[0]) forgetAnnouncementsIfSettled(usernameId);
        }
    }

    private void forgetAnnouncementsIfSettled(String usernameId) {
        ConcurrentHashMap<Integer, QueuedCommand> pending = deferred.get(usernameId);
        if (pending != null && !pending.isEmpty()) return;
        if (dispatchedByPlayer.containsKey(usernameId)) return;
        announcedPayments.remove(usernameId);
    }

    private boolean isAlreadyDispatched(String usernameId, int commandId) {
        Set<Integer> dispatched = dispatchedByPlayer.get(usernameId);
        return dispatched != null && dispatched.contains(commandId);
    }

    private void markDispatched(QueuedCommand command, QueuedPlayer player) {
        String usernameId = player.uuid();

        dispatchedByPlayer.compute(usernameId, (key, dispatched) -> {
            Set<Integer> ids = dispatched == null ? ConcurrentHashMap.<Integer>newKeySet() : dispatched;
            ids.add(command.id());
            return ids;
        });
        ConcurrentHashMap<Integer, QueuedCommand> pending = deferred.get(usernameId);
        if (pending != null) {
            pending.remove(command.id());
        }
    }

    private CompletableFuture<Void> deliver(List<QueuedCommand> commands, QueuedPlayer fallbackPlayer) {
        if (commands == null || commands.isEmpty()) return CompletableFuture.completedFuture(null);

        Map<Integer, List<QueuedCommand>> byPayment = commands.stream()
            .collect(Collectors.groupingBy(QueuedCommand::payment, LinkedHashMap::new, Collectors.toList()));

        Set<Integer> executedIds = new LinkedHashSet<>();
        try {
            for (QueuedCommand command : commands) {
                QueuedPlayer player = command.player() != null ? command.player() : fallbackPlayer;
                if (player == null) {
                    platform.onError("Queued command " + command.id() + " has no player attached", null);
                    continue;
                }

                if (!commandsInFlight.add(command.id())) {
                    continue;
                }

                boolean releaseInFlight = true;
                try {
                    if (isAlreadyDispatched(player.uuid(), command.id())) {
                        executedIds.add(command.id());
                        continue;
                    }

                    CommandConditions conditions = command.conditionsOrNone();

                    if (conditions.slots() > 0 && platform.freeSlots(player.uuid()) < conditions.slots()) {
                        deferred.computeIfAbsent(player.uuid(), key -> new ConcurrentHashMap<>())
                            .put(command.id(), command);
                        continue;
                    }

                    if (conditions.delay() > 0) {
                        releaseInFlight = false;

                        scheduler.schedule(() -> runDelayed(command, player), Duration.ofSeconds(conditions.delay()));
                        continue;
                    }

                    QueuePlatform.DeliveryResult result = run(command, player);
                    if (result != QueuePlatform.DeliveryResult.CANCELLED_REDELIVER) {
                        announcePurchase(command, player, byPayment.get(command.payment()));
                        markDispatched(command, player);
                        executedIds.add(command.id());
                    }
                } catch (RuntimeException ex) {
                    platform.onError("Failed to dispatch command " + command.id(), ex);
                } catch (Throwable error) {
                    platform.onError("Failed to dispatch command " + command.id(), error);
                    throw error;
                } finally {
                    if (releaseInFlight) {
                        commandsInFlight.remove(command.id());
                    }
                }
            }
        } catch (Throwable error) {
            flush(executedIds);
            throw error;
        }
        return flush(executedIds);
    }

    private void announcePurchase(QueuedCommand command, QueuedPlayer player, List<QueuedCommand> ofPayment) {
        boolean[] firstAnnouncement = {false};

        announcedPayments.compute(player.uuid(), (key, announced) -> {
            Set<Integer> payments = announced == null ? ConcurrentHashMap.<Integer>newKeySet() : announced;
            firstAnnouncement[0] = payments.add(command.payment());
            return payments;
        });
        if (!firstAnnouncement[0]) return;
        platform.onPurchase(command.payment(),
            ofPayment == null || ofPayment.isEmpty() ? List.of(command) : ofPayment);
    }

    private void runDelayed(QueuedCommand command, QueuedPlayer player) {
        try {
            QueuePlatform.DeliveryResult result = run(command, player);
            if (result != QueuePlatform.DeliveryResult.CANCELLED_REDELIVER) {
                announcePurchase(command, player, List.of(command));
                markDispatched(command, player);

                flush(Set.of(command.id()))
                    .whenComplete((ignored, error) -> evictIfSettled(player.uuid()));
            } else {
                evictIfSettled(player.uuid());
            }
        } catch (RuntimeException ex) {
            platform.onError("Failed to dispatch delayed command " + command.id(), ex);
        } finally {
            commandsInFlight.remove(command.id());
        }
    }

    private QueuePlatform.DeliveryResult run(QueuedCommand command, QueuedPlayer player) {
        QueuedCommand substituted = new QueuedCommand(
            command.id(),
            substitute(command.command(), player),
            command.payment(),
            command.packageId(),
            command.conditionsOrNone(),
            player);
        return platform.dispatch(substituted, player);
    }

    private CompletableFuture<Void> flush(Collection<Integer> ids) {
        if (ids.isEmpty()) return CompletableFuture.completedFuture(null);
        List<Integer> batch = List.copyOf(ids);
        return api.deleteCommands(batch)
            .thenRun(() -> confirmDeleted(batch))
            .exceptionally(error -> {
                platform.onError("Failed to mark commands executed, Tebex will redeliver them", error);
                return null;
            });
    }

    private static String substitute(String command, QueuedPlayer player) {
        return command
            .replace("{name}", player.name())
            .replace("{id}", player.uuid())
            .replace("{uuid}", player.uuid());
    }
}
