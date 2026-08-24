package com.github.cjh3139.skripttebex.queue;

import com.github.cjh3139.skripttebex.api.TebexPluginApi;
import com.github.cjh3139.skripttebex.api.exception.TebexApiException;
import com.github.cjh3139.skripttebex.api.model.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;

class CommandQueueTest {
    static final class FakeClock implements LongSupplier {
        long nanos;

        @Override
        public long getAsLong() {
            return nanos;
        }

        void advance(Duration amount) {
            nanos += amount.toNanos();
        }

        void passInterval() {
            advance(Duration.ofSeconds(91));
        }
    }

    static final class FakeScheduler implements QueueScheduler {
        record Scheduled(Runnable task, Duration delay) {}

        final List<Scheduled> scheduled = new ArrayList<>();

        @Override
        public void schedule(Runnable task, Duration delay) {
            scheduled.add(new Scheduled(task, delay));
        }

        void runAll() {
            List<Scheduled> copy = List.copyOf(scheduled);
            scheduled.clear();
            copy.forEach(entry -> entry.task().run());
        }
    }

    static final class FakePlatform implements QueuePlatform {
        final Set<String> online = new HashSet<>();
        final Map<String, Integer> slots = new HashMap<>();
        final List<String> executed = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
        final List<Integer> purchases = new ArrayList<>();
        final Set<Integer> throwOnIds = new HashSet<>();

        final Set<Integer> errorOnIds = new HashSet<>();

        final List<List<QueuedCommand>> purchaseCommands = new ArrayList<>();

        final List<Integer> dispatchCalls = new ArrayList<>();

        QueuePlatform.DeliveryResult result = QueuePlatform.DeliveryResult.EXECUTED;

        @Override
        public boolean isOnline(String usernameId) {
            return online.contains(usernameId);
        }

        @Override
        public int freeSlots(String usernameId) {
            return slots.getOrDefault(usernameId, 36);
        }

        @Override
        public QueuePlatform.DeliveryResult dispatch(QueuedCommand command, QueuedPlayer player) {
            dispatchCalls.add(command.id());
            if (errorOnIds.contains(command.id())) {
                throw new AssertionError("error-" + command.id());
            }
            if (throwOnIds.contains(command.id())) {
                throw new RuntimeException("boom-" + command.id());
            }
            if (result == QueuePlatform.DeliveryResult.EXECUTED) {
                executed.add(command.command());
            }
            return result;
        }

        @Override
        public void onPurchase(int paymentId, List<QueuedCommand> commands) {
            purchases.add(paymentId);
            purchaseCommands.add(List.copyOf(commands));
        }

        @Override
        public void onCheck(int duePlayerCount) {}

        @Override
        public void onError(String reason, Throwable cause) {
            errors.add(reason);
        }
    }

    static final class FakeApi extends TebexPluginApi {
        DuePlayers due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of());
        List<QueuedCommand> offline = List.of();
        Map<Integer, List<QueuedCommand>> online = new HashMap<>();
        final List<Integer> deleted = new ArrayList<>();
        RuntimeException failWith;

        CompletableFuture<DuePlayers> duePlayersFuture;
        int duePlayersCalls = 0;

        final Map<Integer, CompletableFuture<List<QueuedCommand>>> onlineCommandsFutures = new HashMap<>();
        final List<Integer> onlineCommandsCalls = new ArrayList<>();

        @Override
        public CompletableFuture<DuePlayers> duePlayers() {
            duePlayersCalls++;
            if (duePlayersFuture != null) return duePlayersFuture;
            if (failWith != null) return CompletableFuture.failedFuture(failWith);
            return CompletableFuture.completedFuture(due);
        }

        @Override
        public CompletableFuture<List<QueuedCommand>> offlineCommands() {
            return CompletableFuture.completedFuture(offline);
        }

        @Override
        public CompletableFuture<List<QueuedCommand>> onlineCommands(int pluginPlayerId) {
            onlineCommandsCalls.add(pluginPlayerId);
            CompletableFuture<List<QueuedCommand>> override = onlineCommandsFutures.get(pluginPlayerId);
            if (override != null) return override;
            return CompletableFuture.completedFuture(online.getOrDefault(pluginPlayerId, List.of()));
        }

        boolean failDeletes = false;

        final List<List<Integer>> deleteAttempts = new ArrayList<>();

        @Override
        public CompletableFuture<Void> deleteCommands(Collection<Integer> ids) {
            deleteAttempts.add(List.copyOf(ids));
            if (failDeletes) {
                return CompletableFuture.failedFuture(new RuntimeException("delete boom"));
            }
            deleted.addAll(ids);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final QueuedPlayer NOTCH = new QueuedPlayer(7, "Notch", "365bfa");

    private static QueuedCommand command(int id, String text, QueuedPlayer player, int delay, int slots) {
        return new QueuedCommand(id, text, 123, 45, new CommandConditions(delay, slots), player);
    }

    @Test
    void respectsNextCheckInterval() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 120, false), List.of());
        CommandQueue queue = new CommandQueue(api, new FakePlatform(), new FakeScheduler());

        queue.checkNow().join();

        assertEquals(120, queue.nextCheckSeconds());
    }

    @Test
    void runsOfflineCommandsWhenExecuteOfflineIsSet() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(true, 90, false), List.of());
        api.offline = List.of(command(1, "say thanks {name}", NOTCH, 0, 0));
        FakePlatform platform = new FakePlatform();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler());

        queue.checkNow().join();

        assertEquals(List.of("say thanks Notch"), platform.executed);
        assertEquals(List.of(1), api.deleted);
        assertEquals(List.of(123), platform.purchases);
    }

    @Test
    void substitutesPlaceholders() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(true, 90, false), List.of());
        api.offline = List.of(command(1, "give {name} {id} {uuid}", NOTCH, 0, 0));
        FakePlatform platform = new FakePlatform();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler());

        queue.checkNow().join();

        assertEquals(List.of("give Notch 365bfa 365bfa"), platform.executed);
    }

    @Test
    void onlineCommandsOnlyRunForOnlinePlayers() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of(NOTCH));
        api.online.put(7, List.of(command(2, "give {name} TNT", NOTCH, 0, 0)));
        FakePlatform platform = new FakePlatform();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler());

        queue.checkNow().join();
        assertTrue(platform.executed.isEmpty());
        assertTrue(api.deleted.isEmpty());

        platform.online.add("365bfa");
        queue.onPlayerJoin("365bfa");

        assertEquals(List.of("give Notch TNT"), platform.executed);
        assertEquals(List.of(2), api.deleted);
    }

    @Test
    void delayConditionSchedulesLater() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(true, 90, false), List.of());
        api.offline = List.of(command(3, "say later", NOTCH, 60, 0));
        FakePlatform platform = new FakePlatform();
        FakeScheduler scheduler = new FakeScheduler();
        CommandQueue queue = new CommandQueue(api, platform, scheduler);

        queue.checkNow().join();
        assertTrue(platform.executed.isEmpty());
        assertTrue(scheduler.scheduled.stream()
            .anyMatch(entry -> entry.delay().equals(Duration.ofSeconds(60))));

        scheduler.runAll();
        assertEquals(List.of("say later"), platform.executed);
        assertEquals(List.of(3), api.deleted);
    }

    @Test
    void slotConditionDefersUntilPlayerHasRoom() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of(NOTCH));
        api.online.put(7, List.of(command(4, "give {name} DIRT", NOTCH, 0, 5)));
        FakePlatform platform = new FakePlatform();
        platform.online.add("365bfa");
        platform.slots.put("365bfa", 2);
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler());

        queue.checkNow().join();
        assertTrue(platform.executed.isEmpty(), "not enough free slots");
        assertTrue(api.deleted.isEmpty(), "undelivered commands must not be deleted");

        platform.slots.put("365bfa", 9);
        api.online.put(7, List.of());
        queue.onPlayerJoin("365bfa");

        assertEquals(List.of("give Notch DIRT"), platform.executed);
        assertEquals(List.of(4), api.deleted);
    }

    @Test
    void cancelledDeliveryIsStillMarkedExecuted() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(true, 90, false), List.of());
        api.offline = List.of(command(5, "say cancelled", NOTCH, 0, 0));
        FakePlatform platform = new FakePlatform();
        platform.result = QueuePlatform.DeliveryResult.CANCELLED_MARK_EXECUTED;
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler());

        queue.checkNow().join();

        assertTrue(platform.executed.isEmpty());
        assertEquals(List.of(5), api.deleted);
    }

    @Test
    void redeliverRequestSkipsDeletion() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(true, 90, false), List.of());
        api.offline = List.of(command(6, "say redeliver", NOTCH, 0, 0));
        FakePlatform platform = new FakePlatform();
        platform.result = QueuePlatform.DeliveryResult.CANCELLED_REDELIVER;
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler());

        queue.checkNow().join();

        assertTrue(api.deleted.isEmpty());
    }

    @Test
    void failureReportsErrorAndBacksOff() {
        FakeApi api = new FakeApi();
        api.failWith = new TebexApiException(500, "boom");
        FakePlatform platform = new FakePlatform();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler());

        queue.checkNow().join();

        assertEquals(1, platform.errors.size());
        assertTrue(queue.nextCheckSeconds() > 90, "backoff should extend the interval");
    }

    @Test
    void stoppedQueueDoesNotReschedule() {
        FakeApi api = new FakeApi();
        FakeScheduler scheduler = new FakeScheduler();
        CommandQueue queue = new CommandQueue(api, new FakePlatform(), scheduler);

        queue.start();
        assertFalse(scheduler.scheduled.isEmpty());

        queue.stop();
        scheduler.runAll();
        assertTrue(scheduler.scheduled.isEmpty());
    }

    @Test
    void slotBlockedCommandDoesNotDuplicateAcrossRepeatedChecks() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of(NOTCH));
        api.online.put(7, List.of(command(10, "give {name} DIRT", NOTCH, 0, 5)));
        FakePlatform platform = new FakePlatform();
        platform.online.add("365bfa");
        platform.slots.put("365bfa", 2);
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler(), clock);

        queue.checkNow().join();
        clock.passInterval();
        queue.checkNow().join();
        clock.passInterval();
        queue.checkNow().join();
        assertTrue(platform.executed.isEmpty(), "still not enough free slots");
        assertTrue(api.deleted.isEmpty());

        platform.slots.put("365bfa", 9);
        api.online.put(7, List.of());
        queue.onPlayerJoin("365bfa");

        assertEquals(List.of("give Notch DIRT"), platform.executed, "dispatched exactly once");
        assertEquals(List.of(10), api.deleted, "deleted exactly once");
    }

    @Test
    void joinDuringInFlightPeriodicCheckDoesNotDuplicateDispatch() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of(NOTCH));
        CompletableFuture<List<QueuedCommand>> manual = new CompletableFuture<>();
        api.onlineCommandsFutures.put(7, manual);
        FakePlatform platform = new FakePlatform();
        platform.online.add("365bfa");
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler());

        CompletableFuture<Void> check = queue.checkNow();

        queue.onPlayerJoin("365bfa");

        manual.complete(List.of(command(11, "give {name} GOLD", NOTCH, 0, 0)));
        check.join();

        assertEquals(List.of("give Notch GOLD"), platform.executed, "dispatched exactly once");
        assertEquals(1, api.onlineCommandsCalls.size(), "only one onlineCommands() call for the racing player");
    }

    @Test
    void dispatchFailureInBatchIsIsolatedAndFlushed() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(true, 90, false), List.of());
        api.offline = List.of(
            command(20, "say first", NOTCH, 0, 0),
            command(21, "say second", NOTCH, 0, 0),
            command(22, "say third", NOTCH, 0, 0));
        FakePlatform platform = new FakePlatform();
        platform.throwOnIds.add(21);
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler());

        queue.checkNow().join();

        assertEquals(List.of("say first", "say third"), platform.executed, "the other commands still dispatch");
        assertEquals(1, platform.errors.size(), "the failure is reported");
        assertEquals(List.of(20, 22), api.deleted, "every dispatched id is still flushed");
    }

    @Test
    void deliveredPlayerWithNothingDeferredIsEvictedFromDueSet() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of(NOTCH));
        api.online.put(7, List.of(command(30, "give {name} APPLE", NOTCH, 0, 0)));
        FakePlatform platform = new FakePlatform();
        platform.online.add("365bfa");
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler(), clock);

        queue.checkNow().join();
        assertEquals(List.of("give Notch APPLE"), platform.executed);
        assertEquals(1, api.onlineCommandsCalls.size());

        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of());
        api.online.put(7, List.of());
        clock.passInterval();
        queue.checkNow().join();

        assertEquals(1, api.onlineCommandsCalls.size(), "evicted player should not be re-polled");
    }

    @Test
    void overlappingChecksCallDuePlayersOnce() {
        FakeApi api = new FakeApi();
        CompletableFuture<DuePlayers> manual = new CompletableFuture<>();
        api.duePlayersFuture = manual;
        CommandQueue queue = new CommandQueue(api, new FakePlatform(), new FakeScheduler());

        CompletableFuture<Void> first = queue.checkNow();
        CompletableFuture<Void> second = queue.checkNow();

        manual.complete(new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of()));
        first.join();
        second.join();

        assertEquals(1, api.duePlayersCalls, "the second call joins the in-flight check instead of polling again");
    }

    @Test
    void joinDuringInFlightCheckWithDeferredCommandDoesNotDuplicateAndReleasesGuard() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of(NOTCH));
        api.online.put(7, List.of(command(40, "give {name} EMERALD", NOTCH, 0, 5)));
        FakePlatform platform = new FakePlatform();
        platform.online.add("365bfa");
        platform.slots.put("365bfa", 2);
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler(), clock);

        queue.checkNow().join();
        assertTrue(platform.executed.isEmpty());

        platform.slots.put("365bfa", 9);
        CompletableFuture<List<QueuedCommand>> manual = new CompletableFuture<>();
        api.onlineCommandsFutures.put(7, manual);

        clock.passInterval();
        CompletableFuture<Void> check = queue.checkNow();
        queue.onPlayerJoin("365bfa");

        manual.complete(List.of(command(40, "give {name} EMERALD", NOTCH, 0, 5)));
        check.join();

        assertEquals(List.of("give Notch EMERALD"), platform.executed, "dispatched exactly once across both paths");
        assertFalse(queue.isCommandInFlight(40), "command id must not be left permanently in the in-flight set");
    }

    @Test
    void deferredCommandDeliveredByPeriodicPathIsNotRedispatchedByLaterJoin() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of(NOTCH));
        api.online.put(7, List.of(command(60, "give {name} DIAMOND", NOTCH, 0, 5)));
        FakePlatform platform = new FakePlatform();
        platform.online.add("365bfa");
        platform.slots.put("365bfa", 2);
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler(), clock);

        queue.checkNow().join();
        assertTrue(platform.executed.isEmpty());

        platform.slots.put("365bfa", 9);
        CompletableFuture<List<QueuedCommand>> manual = new CompletableFuture<>();
        api.onlineCommandsFutures.put(7, manual);
        clock.passInterval();
        CompletableFuture<Void> check = queue.checkNow();
        queue.onPlayerJoin("365bfa");

        manual.complete(List.of(command(60, "give {name} DIAMOND", NOTCH, 0, 5)));
        check.join();
        assertEquals(List.of("give Notch DIAMOND"), platform.executed);

        queue.onPlayerJoin("365bfa");

        assertEquals(List.of("give Notch DIAMOND"), platform.executed, "dispatch was called exactly once");
    }

    @Test
    void commandThatThrewRemainsEligibleForRedeliveryOnLaterCheck() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(true, 90, false), List.of());
        api.offline = List.of(command(70, "say hello", NOTCH, 0, 0));
        FakePlatform platform = new FakePlatform();
        platform.throwOnIds.add(70);
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler(), clock);

        queue.checkNow().join();
        assertTrue(platform.executed.isEmpty(), "the throwing dispatch never actually ran");
        assertEquals(1, platform.errors.size());

        assertTrue(platform.purchases.isEmpty(),
            "a dispatch that threw delivered nothing, so its payment must not be announced");

        platform.throwOnIds.remove(70);
        clock.passInterval();
        queue.checkNow().join();

        assertEquals(List.of("say hello"), platform.executed, "must be attempted again, not suppressed");
        assertEquals(List.of(123), platform.purchases,
            "announced exactly once across the throw and the retry, not once per check");
    }

    @Test
    void cancelledRedeliverDoesNotAnnounceUntilTheCommandReallyDelivers() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(true, 90, false), List.of());
        api.offline = List.of(command(71, "say vip", NOTCH, 0, 0));
        FakePlatform platform = new FakePlatform();

        platform.result = QueuePlatform.DeliveryResult.CANCELLED_REDELIVER;
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler(), clock);

        for (int check = 0; check < 3; check++) {
            clock.passInterval();
            queue.checkNow().join();
        }
        assertTrue(api.deleted.isEmpty(), "nothing was delivered");
        assertTrue(platform.purchases.isEmpty(),
            "a payment whose command keeps being redelivered must not announce once per check");

        platform.result = QueuePlatform.DeliveryResult.EXECUTED;
        clock.passInterval();
        queue.checkNow().join();

        assertEquals(List.of("say vip"), platform.executed);
        assertEquals(List.of(123), platform.purchases, "announced once, on the pass that delivered");
    }

    @Test
    void paymentSplitAcrossDeliverableAndBlockedCommandsAnnouncesOnce() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of(NOTCH));

        api.online.put(7, List.of(
            command(72, "say thanks {name}", NOTCH, 0, 0),
            command(73, "give {name} DIRT", NOTCH, 0, 5)));
        FakePlatform platform = new FakePlatform();
        platform.online.add("365bfa");
        platform.slots.put("365bfa", 2);
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler(), clock);

        queue.checkNow().join();
        assertEquals(List.of("say thanks Notch"), platform.executed);
        assertEquals(List.of(123), platform.purchases, "announced by the half that delivered");
        assertEquals(Set.of(123), queue.announcedPaymentsFor("365bfa"),
            "the record must outlive the batch, since the other half is still waiting");

        platform.slots.put("365bfa", 9);
        api.online.put(7, List.of());
        queue.onPlayerJoin("365bfa");

        assertEquals(List.of("say thanks Notch", "give Notch DIRT"), platform.executed);
        assertEquals(List.of(123), platform.purchases,
            "one payment, one purchase event - not one per delivering batch");
        assertTrue(queue.announcedPaymentsFor("365bfa").isEmpty(),
            "and the record is dropped once the player is settled");
    }

    @Test
    void failedDeleteKeepsSuppressionSoRelistedCommandIsNotDispatchedTwice() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of(NOTCH));
        api.online.put(7, List.of(command(80, "give {name} STONE", NOTCH, 0, 0)));
        api.failDeletes = true;
        FakePlatform platform = new FakePlatform();
        platform.online.add("365bfa");
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler(), clock);

        queue.checkNow().join();

        assertEquals(List.of(80), platform.dispatchCalls, "delivered once");
        assertEquals(List.of(List.of(80)), api.deleteAttempts, "the delete was attempted");
        assertTrue(api.deleted.isEmpty(), "but it never landed");
        assertEquals(1, platform.errors.size(), "the failed delete is reported");
        assertTrue(queue.isDue("365bfa"), "a player with an unconfirmed delete must not be evicted");
        assertEquals(Set.of(80), queue.unconfirmedDispatchesFor("365bfa"),
            "the suppression entry must survive the failed delete");

        clock.passInterval();
        queue.checkNow().join();

        assertEquals(List.of(80), platform.dispatchCalls, "dispatch was called exactly once in total");
        assertEquals(List.of("give Notch STONE"), platform.executed);
        assertEquals(2, api.deleteAttempts.size(), "the delete is retried instead of the command re-running");
    }

    @Test
    void confirmedDeleteEvictsPlayerAndClearsSuppression() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of(NOTCH));
        api.online.put(7, List.of(command(81, "give {name} STONE", NOTCH, 0, 0)));
        FakePlatform platform = new FakePlatform();
        platform.online.add("365bfa");
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler());

        queue.checkNow().join();

        assertEquals(List.of(81), platform.dispatchCalls, "delivered once");
        assertEquals(List.of(81), api.deleted, "the delete landed");
        assertTrue(platform.errors.isEmpty());
        assertFalse(queue.isDue("365bfa"), "a settled player is evicted from the due set");
        assertTrue(queue.unconfirmedDispatchesFor("365bfa").isEmpty(),
            "their suppression entries are cleared once the delete is confirmed");
        assertEquals(0, queue.suppressionEntryCount());
    }

    @Test
    void healthyStoreRetainsNoSuppressionEntriesAcrossCycles() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of(NOTCH));
        FakePlatform platform = new FakePlatform();
        platform.online.add("365bfa");
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler(), clock);

        for (int cycle = 1; cycle <= 4; cycle++) {
            int commandId = 90 + cycle;
            api.online.put(7, List.of(command(commandId, "give {name} ITEM" + cycle, NOTCH, 0, 0)));
            clock.passInterval();
            queue.checkNow().join();

            assertEquals(cycle, platform.dispatchCalls.size(), "one dispatch per cycle");
            assertEquals(cycle, api.deleted.size(), "each delete confirmed");
            assertEquals(0, queue.suppressionEntryCount(),
                "nothing may be retained for a player whose deletes all succeeded (cycle " + cycle + ")");
        }

        assertTrue(platform.errors.isEmpty());
        assertEquals(List.of(91, 92, 93, 94), api.deleted);
    }

    @Test
    void slotBlockedCommandFiresPurchaseEventExactlyOnceAcrossManyChecks() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of(NOTCH));
        api.online.put(7, List.of(command(100, "give {name} DIRT", NOTCH, 0, 5)));
        FakePlatform platform = new FakePlatform();
        platform.online.add("365bfa");
        platform.slots.put("365bfa", 2);
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler(), clock);

        for (int check = 0; check < 4; check++) {
            clock.passInterval();
            queue.checkNow().join();
        }
        assertTrue(platform.executed.isEmpty(), "still blocked on the slot condition");
        assertTrue(platform.purchases.isEmpty(),
            "a command that never got delivered must not announce its payment at all");

        platform.slots.put("365bfa", 9);
        api.online.put(7, List.of());
        queue.onPlayerJoin("365bfa");

        assertEquals(List.of("give Notch DIRT"), platform.executed);
        assertEquals(List.of(123), platform.purchases,
            "the purchase event fires exactly once, on the pass that actually delivered");
    }

    @Test
    void redeliveredCommandAfterFailedDeleteDoesNotRefirePurchaseEvent() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of(NOTCH));
        api.online.put(7, List.of(command(101, "give {name} STONE", NOTCH, 0, 0)));
        api.failDeletes = true;
        FakePlatform platform = new FakePlatform();
        platform.online.add("365bfa");
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler(), clock);

        queue.checkNow().join();
        assertEquals(List.of(123), platform.purchases, "announced once on the delivering pass");

        clock.passInterval();
        queue.checkNow().join();
        clock.passInterval();
        queue.checkNow().join();

        assertEquals(List.of(101), platform.dispatchCalls, "dispatched exactly once in total");
        assertEquals(List.of(123), platform.purchases,
            "a redelivered command must not fire the purchase event again - a script paying out "
                + "on it would pay for one payment three times");
    }

    @Test
    void purchaseEventFiresOncePerPaymentAndCarriesItsCommands() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(true, 90, false), List.of());
        api.offline = List.of(
            command(110, "say one", NOTCH, 0, 0),
            command(111, "say two", NOTCH, 0, 0));
        FakePlatform platform = new FakePlatform();
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler());

        queue.checkNow().join();

        assertEquals(List.of("say one", "say two"), platform.executed);
        assertEquals(List.of(123), platform.purchases, "one payment, one purchase event");
        assertEquals(1, platform.purchaseCommands.size());
        assertEquals(2, platform.purchaseCommands.getFirst().size(),
            "the event still carries every command of that payment");
    }

    @Test
    void repeatedForcedChecksInsideOneIntervalPollOnce() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of());
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, new FakePlatform(), new FakeScheduler(), clock);

        queue.checkNow().join();
        for (int i = 0; i < 20; i++) {
            clock.advance(Duration.ofSeconds(4));
            queue.checkNow().join();
        }

        assertEquals(1, api.duePlayersCalls, "only the first forced check may poll");

        clock.passInterval();
        queue.checkNow().join();
        assertEquals(2, api.duePlayersCalls, "the floor opens again after next_check");
    }

    @Test
    void flooredCheckReturnsACompletedFutureSoCallersDoNotHang() {
        FakeApi api = new FakeApi();
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, new FakePlatform(), new FakeScheduler(), clock);

        queue.checkNow().join();

        CompletableFuture<Void> floored = queue.checkNow();
        assertTrue(floored.isDone(), "a floored check must not leave its caller waiting");
        floored.join();
    }

    @Test
    void scheduledLoopStillPollsOnEveryInterval() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(false, 90, false), List.of());
        FakeScheduler scheduler = new FakeScheduler();
        FakeClock clock = new FakeClock();
        CommandQueue queue = new CommandQueue(api, new FakePlatform(), scheduler, clock);

        queue.start();
        assertEquals(1, api.duePlayersCalls);

        for (int tick = 2; tick <= 4; tick++) {
            assertFalse(scheduler.scheduled.isEmpty(), "the loop keeps rescheduling");
            clock.passInterval();
            scheduler.runAll();
            assertEquals(tick, api.duePlayersCalls, "the scheduled check still polls");
        }
    }

    @Test
    void errorDuringDispatchStillFlushesWhatAlreadyRan() {
        FakeApi api = new FakeApi();
        api.due = new DuePlayers(new DuePlayers.Meta(true, 90, false), List.of());
        api.offline = List.of(
            command(120, "say first", NOTCH, 0, 0),
            command(121, "say boom", NOTCH, 0, 0),
            command(122, "say third", NOTCH, 0, 0));
        FakePlatform platform = new FakePlatform();
        platform.errorOnIds.add(121);
        CommandQueue queue = new CommandQueue(api, platform, new FakeScheduler());

        queue.checkNow().join();

        assertEquals(List.of("say first"), platform.executed);
        assertEquals(List.of(120), api.deleted,
            "the command that did run must be marked executed, or Tebex redelivers and runs it twice");
        assertFalse(platform.errors.isEmpty(), "the Error is reported");
        assertFalse(queue.isCommandInFlight(121), "the failing command is released, not stranded");
    }
}
