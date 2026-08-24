package com.github.cjh3139.skripttebex;

import com.github.cjh3139.skripttebex.api.TebexHeadlessApi;
import com.github.cjh3139.skripttebex.api.TebexPluginApi;
import com.github.cjh3139.skripttebex.api.exception.TebexNotConfiguredException;
import com.github.cjh3139.skripttebex.config.TebexConfig;
import com.github.cjh3139.skripttebex.elements.events.TebexConnectEvent;
import com.github.cjh3139.skripttebex.elements.events.TebexDisconnectEvent;
import com.github.cjh3139.skripttebex.queue.CommandQueue;
import com.github.cjh3139.skripttebex.queue.MinestomQueuePlatform;
import com.github.cjh3139.skripttebex.queue.MinestomQueueScheduler;
import com.github.cjh3139.skripttebex.store.StoreCache;
import com.github.cjh3139.skripttebex.util.TebexEventDispatcher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public final class TebexService {
    private static TebexService instance;

    private final TebexConfig config;
    private final StoreCache cache = new StoreCache();

    private final Object lock = new Object();

    private final AtomicLong generation = new AtomicLong();

    private volatile TebexPluginApi pluginApi;
    private volatile TebexHeadlessApi headlessApi;
    private volatile CommandQueue queue;

    private volatile MinestomQueueScheduler queueScheduler;
    private volatile boolean connected;

    public TebexService(TebexConfig config) {
        this.config = config;
        instance = this;
    }

    public static TebexService get() {
        return instance;
    }

    public boolean isConnected() {
        return connected;
    }

    public TebexPluginApi pluginApi() {
        return pluginApi;
    }

    public TebexHeadlessApi headlessApi() {
        return headlessApi;
    }

    public StoreCache cache() {
        return cache;
    }

    public CommandQueue queue() {
        return queue;
    }

    public CompletableFuture<Void> connect() {
        if (!config.hasSecretKey()) {
            return CompletableFuture.failedFuture(new TebexNotConfiguredException(
                "No Tebex secret key set. Use /tebex secret <key> or edit skript-tebex/config.yml."));
        }

        TebexPluginApi localPluginApi = new TebexPluginApi(config.secretKey());
        TebexHeadlessApi localHeadlessApi = config.publicToken().isBlank()
            ? null : new TebexHeadlessApi(config.publicToken());

        long myGeneration;
        synchronized (lock) {
            myGeneration = generation.incrementAndGet();
            stopAndClearLocked();
            pluginApi = localPluginApi;
            headlessApi = localHeadlessApi;
        }

        long capturedGeneration = myGeneration;
        return localPluginApi.information()
            .thenCompose(information -> {
                synchronized (lock) {
                    if (generation.get() != capturedGeneration) {
                        return CompletableFuture.<Void>completedFuture(null);
                    }
                    cache.setInformation(information);
                    connected = true;
                    MinestomQueueScheduler localScheduler = new MinestomQueueScheduler();
                    queueScheduler = localScheduler;
                    queue = new CommandQueue(localPluginApi, new MinestomQueuePlatform(), localScheduler);
                    queue.start();
                }

                TebexEventDispatcher.fireOnTickThread(new TebexConnectEvent(information));

                return refreshListing(localHeadlessApi);
            })
            .exceptionally(error -> {
                synchronized (lock) {
                    if (generation.get() == capturedGeneration) {
                        connected = false;
                    }
                }
                SkriptTebex.getInstance().logger().error("Could not connect to Tebex: {}", error.getMessage());
                return null;
            });
    }

    public CompletableFuture<Void> refreshListing() {
        return refreshListing(headlessApi);
    }

    private CompletableFuture<Void> refreshListing(TebexHeadlessApi headlessApiToUse) {
        if (headlessApiToUse == null) {
            SkriptTebex.getInstance().logger()
                .warn("No Tebex public token set, store listing and /buy are unavailable.");
            return CompletableFuture.completedFuture(null);
        }
        return headlessApiToUse.categories()
            .thenAccept(cache::setCategories)
            .exceptionally(error -> {
                SkriptTebex.getInstance().logger()
                    .warn("Could not refresh Tebex listing: {}", error.getMessage());
                return null;
            });
    }

    public void disconnect() {
        boolean hadQueue;
        synchronized (lock) {
            generation.incrementAndGet();
            hadQueue = queue != null;
            stopAndClearLocked();
        }

        if (hadQueue) {
            TebexEventDispatcher.fireOnTickThread(new TebexDisconnectEvent());
        }
    }

    private void stopAndClearLocked() {
        if (queue != null) queue.stop();
        queue = null;
        if (queueScheduler != null) queueScheduler.shutdown();
        queueScheduler = null;
        if (pluginApi != null) pluginApi.close();
        pluginApi = null;
        if (headlessApi != null) headlessApi.close();
        headlessApi = null;
        connected = false;
        cache.clear();
    }

    public void setSecretKey(String key) {
        config.setSecretKey(key);
        connect();
    }
}
