package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    // === 검증 메서드들 ===

    /**
     * 사용자 ID 검증
     * @param id 사용자 ID
     * @throws IllegalArgumentException ID가 0 이하인 경우
     */
    private void validateUserId(long id) {
        if (id <= 0L) {
            throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
        }
    }

    /**
     * 금액 검증
     * @param amount 금액
     * @throws IllegalArgumentException 금액이 0 이하인 경우
     */
    private void validateAmount(long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("금액은 양수여야 합니다.");
        }
    }

    // === 비즈니스 로직 ===

    public UserPoint getUserPoint(long id) {
        validateUserId(id);
        return null;
    }

    public List<PointHistory> getPointHistory(long id) {
        validateUserId(id);
        return List.of();
    }

    public UserPoint charge(long id, long amount) {
        validateUserId(id);
        validateAmount(amount);

        // 1. 현재 포인트 조회
        UserPoint currentPoint = userPointTable.selectById(id);

        // 2. 새로운 포인트 = 현재 포인트 + 충전 금액
        long newAmount = currentPoint.point() + amount;

        // 3. 포인트 업데이트
        UserPoint updatedPoint = userPointTable.insertOrUpdate(id, newAmount);

        // 4. 이력 저장
        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, updatedPoint.updateMillis());

        return updatedPoint;
    }

    public UserPoint use(long id, long amount) {
        validateUserId(id);
        return null;
    }
}
