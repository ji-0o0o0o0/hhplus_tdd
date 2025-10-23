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

    /**
     * 포인트 사용여부 검증
     * @param currentPoint 현재 금액
     * @param useAmount 사용량
     * * @throws IllegalArgumentException  "사용 금액 > 잔액"일 때 경우
     */
    private void validateBalance(long currentPoint, long useAmount) {
        if (currentPoint < useAmount) {
            throw new IllegalArgumentException("사용 금액이 잔액보다 큽니다.");
        }
    }

    // === 비즈니스 로직 ===

    public UserPoint getUserPoint(long id) {
        validateUserId(id);

        return userPointTable.selectById(id);
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
        validateAmount(amount);

        // 1. 현재 포인트 조회
        long currentPoint = userPointTable.selectById(id).point();
        //사용여부 확인
        validateBalance(currentPoint,amount);

        //2. 포인트 차감
        long newAmount = currentPoint- amount;
        UserPoint updatePoint = userPointTable.insertOrUpdate(id, newAmount);

        //3. 이력저장
        pointHistoryTable.insert(id, amount, TransactionType.USE, updatePoint.updateMillis());

        return updatePoint;
    }

}
