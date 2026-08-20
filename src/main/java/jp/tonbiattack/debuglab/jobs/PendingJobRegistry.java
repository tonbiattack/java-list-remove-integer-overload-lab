package jp.tonbiattack.debuglab.jobs;

import java.util.ArrayList;
import java.util.List;

/**
 * 待機ジョブIDの取消状態を管理します。
 */
public class PendingJobRegistry {

    private final List<Integer> pendingJobIds = new ArrayList<>(List.of(1, 2, 3));
    private final List<Integer> cancellationHistory = new ArrayList<>();

    public CancellationOutcome cancel(int jobId) {
        if (!pendingJobIds.contains(jobId)) {
            return CancellationOutcome.NOT_FOUND;
        }
        boolean removed = pendingJobIds.remove(Integer.valueOf(jobId));
        if (!removed) {
            return CancellationOutcome.NOT_FOUND;
        }
        Integer removedJobId = jobId;
        cancellationHistory.add(removedJobId);
        return CancellationOutcome.CANCELLED;
    }

    public List<Integer> pendingJobIds() {
        return List.copyOf(pendingJobIds);
    }

    public List<Integer> cancellationHistory() {
        return List.copyOf(cancellationHistory);
    }
}
