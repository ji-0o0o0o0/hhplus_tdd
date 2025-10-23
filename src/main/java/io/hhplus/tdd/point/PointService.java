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

    /**
     * 충전 한도 검증
     * @param amount 충전 금액
     * @throws IllegalArgumentException 충전 금액이 100만원 초과인 경우
     */
    private void validateChargeLimit(long amount) {
        final long MAX_CHARGE_AMOUNT = 1_000_000L;
        if (amount > MAX_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("1회 충전 금액은 100만원을 초과할 수 없습니다.");
        }
    }

    /**
     * 최소 사용 금액 검증
     * @param amount 사용 금액
     * @throws IllegalArgumentException 사용 금액이 100원 미만인 경우
     */
    private void validateMinUseAmount(long amount) {
        final long MIN_USE_AMOUNT = 100L;
        if (amount < MIN_USE_AMOUNT) {
            throw new IllegalArgumentException("포인트는 최소 100원 이상부터 사용 가능합니다.");
        }
    }

    /**
     * 사용 금액 단위 검증
     * @param amount 사용 금액
     * @throws IllegalArgumentException 사용 금액이 10원 단위가 아닌 경우
     */
    private void validateUseUnit(long amount) {
        final long USE_UNIT = 10L;
        if (amount % USE_UNIT != 0) {
            throw new IllegalArgumentException("포인트는 10원 단위로만 사용 가능합니다.");
        }
    }

    // === 비즈니스 로직 ===

    public UserPoint getUserPoint(long id) {
        validateUserId(id);

        return userPointTable.selectById(id);
    }

    public List<PointHistory> getPointHistory(long id) {
        validateUserId(id);

        return pointHistoryTable.selectAllByUserId(id);
    }

    public UserPoint charge(long id, long amount) {
        validateUserId(id);
        validateAmount(amount);
        validateChargeLimit(amount);

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
        validateMinUseAmount(amount);
        validateUseUnit(amount);

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
