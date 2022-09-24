package com.example.account.controller;

import com.example.account.domain.Transaction;
import com.example.account.dto.AccountDto;
import com.example.account.dto.CancelBalance;
import com.example.account.dto.TransactionDto;
import com.example.account.dto.UseBalance;
import com.example.account.service.TransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successUseBalance() throws Exception {
        //given
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
                .willReturn(
                        TransactionDto.builder()
                                .accountNumber("1000000000")
                                .transactionType(USE)
                                .transactionResult(S)
                                .amount(12345L)
                                .balanceSnapshot(90000L)
                                .transactionId("transactionId")
                                .transactedAt(LocalDateTime.now())
                                .build()
                );

        // when

        // then
        mockMvc.perform(
                        post("/transaction/use")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new UseBalance.Request(
                                                1L, "1000000000", 10000L)
                                ))
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.amount").value(12345));

    }

    @Test
    void successCancelBalance() throws Exception {
        // given
        given(transactionService.cancelBalance(anyString(), anyString(), anyLong()))
                .willReturn(
                        TransactionDto.builder()
                                .accountNumber("100000000")
                                .transactionResult(S)
                                .amount(1000L)
                                .transactionId("transactionId")
                                .build()
                );

        // when
        // then
        mockMvc.perform(
                        post("/transaction/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new CancelBalance.Request(
                                                "transactionId",
                                                "1234567890",
                                                12345L
                                        )
                         ))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("100000000"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.amount").value(1000L))
                .andExpect(jsonPath("$.transactionId").value("transactionId"));
    }

    @Test
    void successQueryTransaction() throws Exception {

        // given
        given(transactionService.queryTransaction(anyString()))
                .willReturn(
                        TransactionDto.builder()
                                .accountNumber("1000000000")
                                .transactionType(USE)
                                .transactionResult(S)
                                .amount(12345L)
                                .balanceSnapshot(90000L)
                                .transactionId("transactionId")
                                .transactedAt(LocalDateTime.now())
                                .build()
                );

        // when
        // then
        mockMvc.perform(get("/transaction/12345"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.amount").value(12345));

    }

}