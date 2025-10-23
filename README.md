# 포인트 관리 시스템

TDD 기반으로 구현한 사용자 포인트 충전/사용/조회 시스템입니다.

## 동시성 제어 방식

### 1. 선택한 방식
**`synchronized` 키워드**를 사용한 메서드 레벨 동기화

### 2. 적용 위치
```java
public synchronized UserPoint charge(long id, long amount) {
    // 포인트 충전 로직
}

public synchronized UserPoint use(long id, long amount) {
    // 포인트 사용 로직
}
```

### 3. 동작 원리
`synchronized` 키워드가 붙은 메서드는 한 번에 하나의 스레드만 실행할 수 있습니다.

**예시:**
- 스레드 A가 `charge()` 메서드 실행 중
- 스레드 B가 `charge()` 메서드 호출 시도
- 스레드 B는 A가 완료될 때까지 대기 (blocking)
- 스레드 A 완료 후 스레드 B 실행

이를 통해 **Race Condition**(여러 스레드가 동시에 같은 데이터를 수정하는 문제)을 방지합니다.

### 4. 장점
✅ **구현이 간단** - 메서드에 키워드 하나만 추가하면 됨<br>
✅ **코드 가독성** - 동기화 로직이 명확하게 드러남<br>
✅ **안전성** - JVM이 자동으로 락(lock) 획득/해제를 관리<br>
✅ **데드락 방지** - 메서드 종료 시 자동으로 락 해제

### 5. 단점
❌ **성능 저하** - 모든 요청이 순차적으로 처리되어 동시 처리량 감소<br>
❌ **세밀한 제어 불가** - 메서드 전체가 잠기므로 불필요한 부분까지 동기화<br>
❌ **유연성 부족** - 타임아웃, 조건부 락 등 고급 기능 사용 불가<br>
❌ **확장성 제한** - 단일 서버 내에서만 동작 (분산 환경 불가)

### 6. 다른 동시성 제어 방식 비교

#### 6.1. ReentrantLock
```java
private final ReentrantLock lock = new ReentrantLock();

public UserPoint charge(long id, long amount) {
    lock.lock();
    try {
        // 포인트 충전 로직
    } finally {
        lock.unlock();
    }
}
```
**장점:**
- 타임아웃 설정 가능 (`tryLock(timeout)`)
- 공정성(fairness) 옵션 제공
- 락 상태 확인 가능

**단점:**
- 코드가 복잡해짐
- 수동으로 락 해제 필요 (finally 블록 필수)

#### 6.2. 비관적 락 (Pessimistic Lock)
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
UserPoint findById(Long id);
```
**장점:**
- DB 레벨에서 락 제공
- 분산 환경에서도 동작
- 트랜잭션과 통합

**단점:**
- DB 성능에 영향
- 데드락 위험 증가
- DB에 종속적

#### 6.3. 낙관적 락 (Optimistic Lock)
```java
@Version
private Long version;
```
**장점:**
- 읽기 작업에 락이 없어 성능 좋음
- 충돌이 적을 때 효율적

**단점:**
- 충돌 시 재시도 로직 필요
- 충돌이 많으면 오히려 비효율적

### 7. 동시성 테스트

동시성 제어를 검증하기 위해 다음 3가지 테스트를 작성했습니다:

#### 7.1. 동시에 포인트 충전
10개의 스레드가 동시에 같은 사용자에게 1000원씩 충전합니다.

```java
@Test
void 동시에_포인트_충전() throws InterruptedException {
    long userId = 1L;
    int threadCount = 10;
    long chargeAmount = 1000L;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                pointService.charge(userId, chargeAmount);
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();

    UserPoint result = pointService.getUserPoint(userId);
    assertThat(result.point()).isEqualTo(10000L);  // 기대: 10,000원
}
```

#### 7.2. 동시에 포인트 사용
초기 10,000원이 있는 상태에서 10개의 스레드가 동시에 500원씩 사용합니다.

```java
@Test
void 동시에_포인트_사용() throws InterruptedException {
    long userId = 2L;
    pointService.charge(userId, 10000L);  // 초기 충전

    int threadCount = 10;
    long useAmount = 500L;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                pointService.use(userId, useAmount);
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();

    UserPoint result = pointService.getUserPoint(userId);
    assertThat(result.point()).isEqualTo(5000L);  // 기대: 5,000원
}
```

#### 7.3. 동시에 충전과 사용
5개 스레드는 충전, 5개 스레드는 사용을 동시에 수행합니다.

```java
@Test
void 동시에_충전과_사용() throws InterruptedException {
    long userId = 3L;
    pointService.charge(userId, 10000L);  // 초기 충전

    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount / 2; i++) {
        // 충전 스레드
        executor.submit(() -> {
            try {
                pointService.charge(userId, 1000L);
            } finally {
                latch.countDown();
            }
        });

        // 사용 스레드
        executor.submit(() -> {
            try {
                pointService.use(userId, 500L);
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();

    UserPoint result = pointService.getUserPoint(userId);
    assertThat(result.point()).isEqualTo(12500L);  // 기대: 12,500원
}
```

### 8. 결론

현재 프로젝트는 **단일 서버 환경**에서 **간단한 동시성 제어**가 필요하므로 `synchronized` 키워드를 선택했습니다.

**향후 개선 방안:**
- 트래픽 증가 시 → Redis 기반 분산 락 도입
- 세밀한 제어 필요 시 → ReentrantLock으로 변경
- 분산 환경 구축 시 → DB 락(비관적/낙관적) 고려

## 기술 스택
- Java 17
- Spring Boot
- Gradle
- JUnit 5
- AssertJ

## 테스트 실행
```bash
# 전체 테스트
./gradlew test

# 특정 테스트
./gradlew test --tests PointServiceTest
./gradlew test --tests PointControllerIntegrationTest
./gradlew test --tests PointConcurrencyTest
```
