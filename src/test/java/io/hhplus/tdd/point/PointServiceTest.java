package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
    /*성공*/
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

    /*예외*/
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

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -100L})
    @DisplayName("사용자 ID가 0 이하일 때 예외 발생")
    void charge_잘못된사용자ID_예외발생(long invalidUserId) {
        // When & Then
        assertThatThrownBy(() -> pointService.charge(invalidUserId, 500L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("양수");
    }

    @ParameterizedTest(name = "충전 금액이 {0}원일 때 최대 한도 초과 예외가 발생한다")
    @ValueSource(longs = {1_000_001L, 2_000_000L, 10_000_000L})
    @DisplayName("충전 금액이 100만원 초과일 때 예외가 발생한다")
    void charge_충전금액이_최대한도_초과일때_예외발생(long overLimitAmount) {
        // Given
        long userId = 1L;

        // When & Then
        assertThatThrownBy(() -> pointService.charge(userId, overLimitAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100만원");
    }


    /*포인트 사용*/
    /*성공*/
    @ParameterizedTest(name = "{0}원 충전 후 {1}원 사용하면 {2}원 남는다")
    @CsvSource({
            "1000, 200, 800",
            "5000, 1000, 4000",
            "10000, 3000, 7000"
    })
    @DisplayName("포인트 사용 후 잔액이 정확히 계산된다")
    void use_정상사용_잔액계산(long chargeAmount, long useAmount, long expectedLeft) {

        //Given
        long userId = 1L;

        //When
        UserPoint result = pointService.charge(userId, chargeAmount);
        result = pointService.use(userId, useAmount);

        //Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(expectedLeft);

    }

    /*예외*/
    @ParameterizedTest
    @ValueSource(longs = {0L, -1000L, -5000L})
    @DisplayName("사용 금액이 0 이하일 때 예외 발생")
    void use_사용금액이_0이하일때_예외발생(long invalidAmount) {
        // 0원, -100원 사용 시도
        //Given
        long userId = 1L;
        pointService.charge(userId, 500L);

        // When & Then
        assertThatThrownBy(() -> pointService.use(userId, invalidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("양수");
    }

    @ParameterizedTest(name = "잔액 {0}원보다 많은 금액 ({1}원) 사용하면 예외가 발생한다. ")
    @CsvSource({
            "50, 200",
            "500, 1000",
            "10, 3000"
    })
    @DisplayName("잔액보다 많은 금액 사용 시 예외 발생")
    void use_잔액부족_예외발생(long chargeAmount, long useAmount) {
        // 500원 있는데 1000원 사용 시도
        // Given
        long userId = 1L;
        pointService.charge(userId, chargeAmount);

        // When & Then
        assertThatThrownBy(() -> pointService.use(userId, useAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔액");
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -100L})
    @DisplayName("사용자 ID가 0 이하일 때 예외 발생")
    void use_잘못된사용자ID_예외발생(long invalidUserId) {
        // Given
        //long userId = 0L;

        // When & Then
        assertThatThrownBy(() -> pointService.use(invalidUserId, 500L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("양수");
    }

    @ParameterizedTest(name = "사용 금액이 {0}원일 때 최소 사용 금액 미달 예외가 발생한다")
    @ValueSource(longs = {1L, 50L, 99L})
    @DisplayName("사용 금액이 100원 미만일 때 예외가 발생한다")
    void use_사용금액이_최소금액_미달일때_예외발생(long underMinAmount) {
        // Given
        long userId = 1L;
        pointService.charge(userId, 10000L);

        // When & Then
        assertThatThrownBy(() -> pointService.use(userId, underMinAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100원");
    }

    @ParameterizedTest(name = "사용 금액이 {0}원일 때 10원 단위가 아니므로 예외가 발생한다")
    @ValueSource(longs = {105L, 1234L, 5678L})
    @DisplayName("사용 금액이 10원 단위가 아닐 때 예외가 발생한다")
    void use_사용금액이_10원단위가_아닐때_예외발생(long invalidUnitAmount) {
        // Given
        long userId = 1L;
        pointService.charge(userId, 10000L);

        // When & Then
        assertThatThrownBy(() -> pointService.use(userId, invalidUnitAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10원 단위");
    }

    /*포인트 조회*/
    /*성공*/
    @Test
    @DisplayName("포인트를 충전한 후 조회하면 정확한 금액이 반환된다")
    void getUserPoint_포인트_조회(){
        //Given
        long userId = 1L;
        long chargeAmount = 500L;
        pointService.charge(userId, chargeAmount);

        //When
        UserPoint result = pointService.getUserPoint(userId);

        //Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(chargeAmount);
    }
    @Test
    @DisplayName("포인트를 충전하지 않은 사용자를 조회하면 0원이 반환된다")
    void getUserPoint_신규사용자_0원반환(){
        //Given
        long userId = 999L;

        //When
        UserPoint result = pointService.getUserPoint(userId);

        //Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(0L);
    }

    /*예외*/
    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -100L})
    @DisplayName("사용자 ID가 0 이하일 때 예외 발생")
    void getUserPoint_잘못된사용자ID_예외발생(long invalidUserId) {
        // When & Then
        assertThatThrownBy(() -> pointService.getUserPoint(invalidUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("양수");
    }

    //포인트 내역 조회
    /*성공*/
    @Test
    @DisplayName("히스토리 없는 신규 사용자 히스토리 조회")
    void getPointHistory_히스토리_없는_신규_사용자_히스토리_조회(){
        //Given
        long userId = 1L;

        //When
        List<PointHistory> result = pointService.getPointHistory(userId);

        //Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("포인트 충전 후 히스토리 조회")
    void getPointHistory_포인트_충전_후_히스토리_조회(){
        //Given
        long userId = 1L;
        long chargeAmount = 500L;
        pointService.charge(userId, chargeAmount);

        //When
        List<PointHistory> result = pointService.getPointHistory(userId);
        //Then
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).userId()).isEqualTo(userId);
        assertThat(result.get(0).type()).isEqualTo(TransactionType.CHARGE);
    }

    @Test
    @DisplayName("포인트 사용 후 히스토리 조회")
    void getPointHistory_포인트_사용_후_히스토리_조회(){
        //Given
        long userId = 1L;
        long chargeAmount = 500L;
        long useAmount = 100L;
        pointService.charge(userId, chargeAmount);
        pointService.use(userId, useAmount);

        //When
        List<PointHistory> result = pointService.getPointHistory(userId);
        //Then
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).userId()).isEqualTo(userId);
        assertThat(result.get(0).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(result.get(1).type()).isEqualTo(TransactionType.USE);
    }

    /*예외*/
    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -100L})
    @DisplayName("사용자 ID가 0 이하일 때 예외 발생")
    void getPointHistory_잘못된사용자ID_예외발생(long invalidUserId) {
        // When & Then
        assertThatThrownBy(() -> pointService.getPointHistory(invalidUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("양수");
    }
}
