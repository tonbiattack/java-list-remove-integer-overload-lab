package jp.tonbiattack.debuglab.jobs;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class PendingJobRegistryTest {

    @Test
    void cancel_removesTheRequestedJobIdRatherThanTheSameIndex() {
        PendingJobRegistry registry = new PendingJobRegistry();

        CancellationOutcome outcome = registry.cancel(1);

        assertAll(
                () -> assertEquals(CancellationOutcome.CANCELLED, outcome,
                        "登録済みのIDの取消は成功する"),
                () -> assertEquals(List.of(2, 3), registry.pendingJobIds(),
                        "ID 1を取り除き、ID 2と3を待機状態に残す"),
                () -> assertEquals(List.of(1), registry.cancellationHistory(),
                        "取消履歴には要求したID 1を記録する")
        );
    }

    @Test
    void cancel_unknownId_preservesTheQueueAndHistory() {
        PendingJobRegistry registry = new PendingJobRegistry();

        CancellationOutcome outcome = registry.cancel(9);

        assertAll(
                () -> assertEquals(CancellationOutcome.NOT_FOUND, outcome),
                () -> assertEquals(List.of(1, 2, 3), registry.pendingJobIds()),
                () -> assertEquals(List.of(), registry.cancellationHistory())
        );
    }
}
