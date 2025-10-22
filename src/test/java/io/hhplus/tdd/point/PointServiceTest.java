package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PointServiceTest {

    private PointService pointService;
    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @ParameterizedTest(name = "{0}원을 충전하면 포인트가 {0}원이 된다")
    @ValueSource(longs = {1000L, 2000L, 5000L})
    @DisplayName("포인트를 충전하면 금액이 정상적으로 증가한다")
    void charge_포인트를_충전하면_금액이_정상적으로_증가한다(long chargeAmount) {
        // Given
        long userId = 1L;

        // When
        UserPoint result = pointService.charge(userId, chargeAmount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(chargeAmount);
    }

    @ParameterizedTest(name = "충전 금액이 {0}원일 때 예외가 발생한다")
    @ValueSource(longs = {0L, -1000L, -5000L})
    @DisplayName("충전 금액이 0 이하일 때 예외가 발생한다")
    void charge_충전금액이_0이하일때_예외발생(long invalidAmount) {
        // Given
        long userId = 1L;

        // When & Then
        assertThatThrownBy(() -> pointService.charge(userId, invalidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금액");
    }
}
