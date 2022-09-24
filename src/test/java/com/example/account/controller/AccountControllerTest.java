package com.example.account.controller;

import com.example.account.domain.Account;
import com.example.account.dto.AccountDto;
import com.example.account.dto.CreateAccount;
import com.example.account.dto.DeleteAccount;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.service.AccountService;
import com.example.account.service.RedisTestService;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @MockBean
    private RedisTestService redisTestService;

    @MockBean
    private AccountService accountService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("계좌 생성 성공")
    void successCreateAccount() throws Exception {
        // given
        given(accountService.createAccount(anyLong(), anyLong()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());

        // when
        // then
        mockMvc.perform(post("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAccount.Request(1L, 100L)
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"));
    }

    @Test
    @DisplayName("계좌 해지 성공")
    void successDeleteAccount() throws Exception {
        // given
        given(accountService.deleteAccount(anyLong(), anyString()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1111111111")
                        .registeredAt(LocalDateTime.of(2022, 8, 21, 14, 23))
                        .unRegisteredAt(LocalDateTime.now())
                        .build());

        // when
        // then
        mockMvc.perform(delete("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DeleteAccount.Request(33333L, "1234567890")
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1111111111"))
                .andDo(print());
    }

    @Test
    @DisplayName("계좌 확인 성공")
    void successGetAccountByUserId() throws Exception {

        List<AccountDto> accountDtos = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String accountNumber = "10000000" + i;
            Long balance = i * 10000L;
            accountDtos.add(AccountDto.builder()
                    .accountNumber(accountNumber)
                    .balance(balance).build());
        }

        // given
        given(accountService.getAccountByUserId(anyLong()))
                .willReturn(accountDtos);

        // when
        // then
        mockMvc.perform(get("/account?user_id=1"))
                .andDo(print())
                .andExpect(jsonPath("$[0].accountNumber").value("100000000"))
                .andExpect(jsonPath("$[0].balance").value(0L))
                .andExpect(jsonPath("$[1].accountNumber").value("100000001"))
                .andExpect(jsonPath("$[1].balance").value(10000L))
                .andExpect(jsonPath("$[2].accountNumber").value("100000002"))
                .andExpect(jsonPath("$[2].balance").value(20000L));

    }

    @Test
    void failGetAccount() throws Exception {
        // given
        given(accountService.getAccountByUserId(anyLong()))
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        // when
        // then
        mockMvc.perform(get("/account?user_id=1"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("계좌가 없습니다."));

    }

}