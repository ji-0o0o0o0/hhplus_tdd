package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class PointConcurrencyTest {
    @Autowired
    private PointService pointService;

    /* 메서드명: 동시에_포인트_충전()
    - 내용: userId = 1L 인 사용자가 초기 포인트 0, 동시에 10번(thread 10개) 1000원씩 충전
    - 검증: 최종 포인트 조회 시 10000원 확인
    * */
    @Test
    @DisplayName("동시에 포인트 충전")
    void 동시에_포인트_충전()  throws InterruptedException { // latch.await();  // ← 이 메서드가 InterruptedException을 던짐
        //1. 준비
        long userId = 1L;
        int threadCnt = 10;
        long chargeAmount = 1000L;
        long resultAmount =chargeAmount*threadCnt;

                //2. ExcutorService로 멀티스레드 실행
        ExecutorService executor = Executors.newFixedThreadPool(threadCnt); //-> ExecutorService: 멀티스레드 관리함
        CountDownLatch latch = new CountDownLatch(threadCnt); //-> CountDownLatch :  모든 스레드 완료 대기

        //3. 10번 충전
        for (int i = 0; i < threadCnt; i++) {
            executor.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);
                }finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); //모든 스레드 완료 대기

        //4. 검증
        UserPoint result = pointService.getUserPoint(userId);
        assertThat(result.point()).isEqualTo(resultAmount);

    }
    /* 메서드명: 동시에_포인트_사용()
    - 내용: userId = 2L 인 사용자가 초기 포인트 10000원, 동시에 10번(thread 10개) 500원씩 사용
    - 검증: 최종 포인트 조회 시 5000원 확인
    * */
    @Test
    @DisplayName("동시에 포인트 사용")
    void 동시에_포인트_사용()  throws InterruptedException {
        //1. 준비
        long userId = 2L;
        int threadCnt = 10;
        long chargeAmount = 10000L;
        long useAmount = 500L;
        long resultAmount = chargeAmount-(useAmount*threadCnt);

        //충전
        pointService.charge(userId, chargeAmount);

        //2. ExecutorService로 멀티스레드 실행
        ExecutorService executor = Executors.newFixedThreadPool(threadCnt);
        CountDownLatch latch = new CountDownLatch(threadCnt);

        //3. 10번 사용
        for (int i = 0; i < threadCnt; i++) {
            executor.submit(() -> {
                try {
                    pointService.use(userId, useAmount);
                }finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        //4. 검증
        UserPoint result = pointService.getUserPoint(userId);
        assertThat(result.point()).isEqualTo(resultAmount);
    }


    /* 메서드명: 동시에_충전과_사용()
    - 내용: userId = 3L 인 사용자가 초기 포인트 10000, 동시에 1000원 5번 충전 ,500원 5번 사용  1000원씩 충전 시
    - 검증: 최종 포인트 조회 시 12500원 확인
    * */
    @Test
    @DisplayName("동시에 충전과 사용")
    void 동시에_충전과_사용() throws InterruptedException {
        //1. 준비
        long userId = 3L;
        int threadCnt = 10;
        long initAmount = 10000L;
        long chargeAmount = 1000L;
        long useAmount = 500L;
        long resultAmount = initAmount+chargeAmount*threadCnt/2-useAmount*threadCnt/2;

        //초기 충전
        pointService.charge(userId, initAmount);

        //2. 멀티스레드 실행
        ExecutorService excutor = Executors.newFixedThreadPool(threadCnt);
        CountDownLatch latch = new CountDownLatch(threadCnt);

        //3. 동시 충전 및 사용
        for (int i = 0; i < threadCnt/2; i++) {
            //충전 스레드 5개
            excutor.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);
                }finally {
                    latch.countDown();
                }
            });
            //사용 스레드 5개
            excutor.submit(() -> {
                try {
                    pointService.use(userId, useAmount);
                }finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        //4. 검증
        UserPoint result = pointService.getUserPoint(userId);
        assertThat(result.point()).isEqualTo(resultAmount);
    }

}
