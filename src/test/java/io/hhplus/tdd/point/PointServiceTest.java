package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PointServiceTest {

    private static final Logger log = LoggerFactory.getLogger(PointServiceTest.class);
    private PointService pointService;
    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    /*포인트 충전*/
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

    /*포인트 사용*/
    @Test
    @DisplayName("1000point가 있을때 200point를 사용하면 800point  남는다.")
    void use_1000point가_있을때_200point를_사용하면_800point_남는다(){
        //Given
        long userId = 1L;
        long chargeAmount = 1000;
        long useAmount = 200;
        long leftAmount = 800;

        //When
        UserPoint result = pointService.charge(userId, chargeAmount);
        result = pointService.use(userId, useAmount);

        //Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(leftAmount);

    }

    @Test
    @DisplayName("사용자 ID가 0 이하일 때 예외 발생")
    void use_잘못된사용자ID_예외발생() {
        // userId = 0, -1 일 때
        // Given
        long userId = 0L;

        // When & Then
        assertThatThrownBy(() -> pointService.use(userId, 500L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("양수");
    }

    @Test
    @DisplayName("사용 금액이 0 이하일 때 예외 발생")
    void use_사용금액이_0이하일때_예외발생() {
        // 0원, -100원 사용 시도
        //Given
        long userId = 1L;
        long useAmount = 0;
        pointService.charge(userId, 500L);

        // When & Then
        assertThatThrownBy(() -> pointService.use(userId, useAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("양수");
    }

    @Test
    @DisplayName("잔액보다 많은 금액 사용 시 예외 발생")
    void use_잔액부족_예외발생() {
        // 500원 있는데 1000원 사용 시도
        // Given
        long userId = 1L;
        pointService.charge(userId, 500L);

        // When & Then
        assertThatThrownBy(() -> pointService.use(userId, 1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔액");
    }



}
