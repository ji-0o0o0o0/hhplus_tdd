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


}

