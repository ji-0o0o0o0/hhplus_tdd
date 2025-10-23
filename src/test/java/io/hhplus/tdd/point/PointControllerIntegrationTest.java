package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.assertj.core.util.diff.Patch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@SpringBootTest  // 스프링 컨텍스트 전체를 띄움
@AutoConfigureMockMvc  // MockMvc 자동 설정
public class PointControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc; // HTTP 요청을 시뮬레이션하는 도구(가짜 HTTP 요청을 만드는 도구)

    /*포인트 충전*/
    /* 메서드명: chargePoint_성공()
     - 내용: PATCH /point/{id}/charge 호출해서 5000원 충전
     - 검증: 응답 상태 200, id 일치, point가 5000인지 확인
    * */
    @Test
    @DisplayName("포인트 충전 API 호출 시 정상적으로 충전된다.")
    void chargePoint_성공() throws Exception{
        //Given
        long userId = 1L;
        long chargeAmount = 5000L;
        //when & Then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk()) // 응답 상태 200인지 확인
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(chargeAmount));

        /*  - perform(...) ← HTTP 요청 설정
            - .andExpect(...) ← 응답 검증
            - jsonPath("$.id"): JSON에서 id 필드 선택*/
    }
    /* 메서드명: chargePoint_한도초과_실패()
     - 내용: PATCH /point/{id}/charge 호출해서 1,500,000원 충전(한도 100만원)
     - 검증: 응답 상태 500 에러
    * */
    @Test
    @DisplayName("충전 한도 초과 시 예외 발생")
    void chargePoint_한도초과_실패() throws Exception {
        //Given
        long userId = 1L;
        long overLimitAmount = 1_500_000L;

        //When&Then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(overLimitAmount)))
                .andExpect(status().is5xxServerError()) ;// 응답 상태 200인지 확인
    }

    /*포인트 사용*/
    /* 메서드명: usePoint_성공()
     - 내용: PATCH /point/{id}/use 호출해서 5000원 충전 후 3000원 사용 성공
     - 검증: 응답 상태 200, id 일치, point가 2000인지 확인
    * */
    @Test
    @DisplayName("포인트 충전 후 포인트 사용API 호출 시 정상적으로 사용된다.")
    void usePoint_성공()throws Exception{
        //Given
        long userId = 2L; //충돌 방지
        long chargeAmount = 5000L;
        long useAmount = 3000L;

        //충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargeAmount)));

        //When&Then
        mockMvc.perform(patch("/point/{id}/use",userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(chargeAmount-useAmount));

    }
    /* 메서드명: usePoint_잔액부족_실패()
     - 내용: PATCH /point/{id}/use 호출해서 충전 안하고 1000원 사용
     - 검증: 응답 상태 500 에러
    * */
    @Test
    @DisplayName("잔액 부족 시 예외 발생")
    void usePoint_잔액부족_실패()throws Exception{
        //Given
        long userId = 3L; //충돌 방지
        long useAmount = 3000L;

        //When&Then
        mockMvc.perform(patch("/point/{id}/use",userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().is5xxServerError());
    }
    /* 메서드명: usePoint_최소사용금액미달_실패()
     - 내용: PATCH /point/{id}/use 호출해서 5000원 충전 후 50원 사용(최소 사용금액 100원)
     - 검증: 응답 상태 500 에러
    * */
    @Test
    @DisplayName("최소 사용 금액 미달 시 예외 발생")
    void usePoint_최소사용금액미달_실패()throws Exception{
        //Given
        long userId = 4L; //충돌 방지
        long chargeAmount = 5000L;
        long useAmount = 50L;

        //충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargeAmount)));

        //When&Then
        mockMvc.perform(patch("/point/{id}/use",userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().is5xxServerError());

    }
    /* 메서드명: usePoint_10원단위위반_실패()
     - 내용: PATCH /point/{id}/use 호출해서 5000원 충전 후 4536원 사용(10원 단위로만 사용 가능)
     - 검증: 응답 상태 500 에러
    * */
    @Test
    @DisplayName("10원 단위 위반 시 예외 발생")
    void usePoint_10원단위위반_실패()throws Exception{
        //Given
        long userId = 5L; //충돌 방지
        long chargeAmount = 5000L;
        long useAmount = 4536L;

        //충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargeAmount)));

        //When&Then
        mockMvc.perform(patch("/point/{id}/use",userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().is5xxServerError());

    }

    /*포인트 조회*/
    /* 메서드명: getUserPoint_충전후조회_성공()
     - 내용: Get /point/{id} 호출해서 20000원 충전 후 조회
     - 검증: 응답 상태 200, id 일치, point가 20000원인지 확인
    * */
    @Test
    @DisplayName("포인트 충전 후 조회 시 정확한 잔액이 반환된다")
    void getUserPoint_충전후조회_성공()throws Exception{
        //Given
        long userId = 6L; //충돌 방지
        long chargeAmount = 20000L;

        //충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargeAmount)));

        //When&Then
        mockMvc.perform(get("/point/{id}",userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(chargeAmount));

    }


}

