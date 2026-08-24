package com.github.cjh3139.skripttebex.queue;

import com.github.cjh3139.skripttebex.api.model.QueuedCommand;
import com.github.cjh3139.skripttebex.api.model.QueuedPlayer;

import java.util.List;

public interface QueuePlatform {
    enum DeliveryResult {
        EXECUTED,

        CANCELLED_MARK_EXECUTED,

        CANCELLED_REDELIVER
    }

    boolean isOnline(String usernameId);

    int freeSlots(String usernameId);

    DeliveryResult dispatch(QueuedCommand command, QueuedPlayer player);

    void onPurchase(int paymentId, List<QueuedCommand> commands);

    void onCheck(int duePlayerCount);

    void onError(String reason, Throwable cause);
}
