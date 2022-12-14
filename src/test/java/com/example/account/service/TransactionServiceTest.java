package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    public static final long USE_AMOUNT = 200L;
    public static final long CANCEL_AMOUNT = 1000L;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void useBalanceSuccess() {
        // given
        AccountUser harry = AccountUser.builder()
                .name("harry").build();
        harry.setId(17L);

        Account account = Account.builder()
                .accountUser(harry)
                .accountNumber("1000000012")
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(harry));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResult(S)
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        TransactionDto transactionDto = transactionService.useBalance(1L,
                "1000000012", USE_AMOUNT);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(9800L, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResult());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());

    }

    @Test
    @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
    void useBalanceFailed_UserNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L,
                        "1000000012", USE_AMOUNT));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
    void useBalanceFailed_AccountNotFound() {
        // given
        AccountUser harry = AccountUser.builder()
                .name("harry").build();
        harry.setId(17L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(harry));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L,
                        "1000000012", USE_AMOUNT));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("?????? ????????? ?????? - ?????? ?????? ??????")
    void useBalanceFailed_UserUnMatch() {
        // given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi").build();
        pobi.setId(12L);

        AccountUser harry = AccountUser.builder()
                .name("harry").build();
        harry.setId(17L);

        Account account = Account.builder()
                .accountUser(pobi)
                .accountNumber("1000000012")
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(harry));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L,
                        "1000000012", USE_AMOUNT));

        // then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());

    }

    @Test
    @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
    void useBalanceFailed_AlreadyUnregistered() {
        // given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user)
                .accountNumber("1000000012")
                .balance(0L)
                .accountStatus(AccountStatus.UNREGISTERED)
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L,
                        "1000000012", USE_AMOUNT));

        // then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());

    }

    @Test
    @DisplayName("?????? ????????? ???????????? ??? ?????? - ?????? ?????? ??????")
    void useBalanceFailed_AmountExceedBalance() {
        // given
        AccountUser harry = AccountUser.builder()
                .name("harry").build();
        harry.setId(17L);

        Account account = Account.builder()
                .accountUser(harry)
                .accountNumber("1000000012")
                .balance(100L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(harry));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L,
                        "1000000012", USE_AMOUNT));

        // then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        verify(transactionRepository, times(0)).save(any());

    }

    @Test
    @DisplayName("?????? ???????????? ?????? ?????? - ?????? ?????? ??????")
    void saveFailedUseTransaction() {
        // given
        AccountUser harry = AccountUser.builder()
                .name("harry").build();
        harry.setId(17L);

        Account account = Account.builder()
                .accountUser(harry)
                .accountNumber("1000000012")
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResult(S)
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        transactionService.saveFailedUseTransaction("1000000000", USE_AMOUNT);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(F, captor.getValue().getTransactionResult());

    }

    @Test
    @DisplayName("?????? ?????? ?????? ??????")
    void cancelBalanceSuccess() {
        // given
        AccountUser harry = AccountUser.builder()
                .name("harry").build();
        harry.setId(17L);

        Account account = Account.builder()
                .accountUser(harry)
                .accountNumber("1234567890")
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(
                        Optional.of(Transaction.builder()
                                .transactionId("transactionId")
                                .account(account)
                                .amount(1000L)
                                .transactedAt(LocalDateTime.now())
                                .build())
                );

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(CANCEL)
                        .transactionResult(S)
                        .amount(1000L)
                        .balanceSnapshot(11000L)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        TransactionDto transactionDto = transactionService.cancelBalance(
                "transactionId", "1234567890", CANCEL_AMOUNT
        );

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(CANCEL_AMOUNT, captor.getValue().getAmount());
        assertEquals(11000L, captor.getValue().getBalanceSnapshot());
        assertEquals(S, captor.getValue().getTransactionResult());
        assertEquals(CANCEL, captor.getValue().getTransactionType());
        assertEquals(S, transactionDto.getTransactionResult());
        assertEquals(CANCEL, transactionDto.getTransactionType());

    }

    @Test
    @DisplayName("????????? ?????? - ?????? ?????? ?????? ??????")
    void cancelBalanceFailed_TransactionNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", CANCEL_AMOUNT
                ));

        // then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("?????? ?????? ?????? - ?????? ?????? ?????? ??????")
    void cancelBalanceFailed_AccountNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(
                        Optional.of(Transaction.builder()
                                .transactionId("transactionId")
                                .account(null)
                                .amount(1000L)
                                .transactedAt(LocalDateTime.now())
                                .build())
                );

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", CANCEL_AMOUNT
                ));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("?????? ????????? ????????? ????????? ????????? ???????????? ????????????. - ?????? ?????? ?????? ??????")
    void cancelBalanceFailed_AccountUnMatch() {
        // given
        AccountUser harry = AccountUser.builder()
                .name("harry").build();
        harry.setId(17L);

        Account accountA = Account.builder()
                .accountUser(harry)
                .accountNumber("1234567890")
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();
        accountA.setId(12L);

        Account accountB = Account.builder()
                .accountUser(harry)
                .accountNumber("1234567890")
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();
        accountB.setId(13L);

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(
                        Optional.of(Transaction.builder()
                                .transactionId("transactionId")
                                .account(accountA)
                                .amount(1000L)
                                .transactedAt(LocalDateTime.now())
                                .build())
                );

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountB));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", CANCEL_AMOUNT
                ));

        // then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());

    }

    @Test
    @DisplayName("1?????? ?????? ????????? ?????? ?????? ????????? - ?????? ?????? ?????? ??????")
    void cancelBalanceFailed_tooOldToCancel() {
        // given
        AccountUser harry = AccountUser.builder()
                .name("harry").build();
        harry.setId(17L);

        Account account = Account.builder()
                .accountUser(harry)
                .accountNumber("1234567890")
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(
                        Optional.of(Transaction.builder()
                                .transactionId("transactionId")
                                .account(account)
                                .amount(1000L)
                                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                                .build())
                );

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", CANCEL_AMOUNT
                ));

        // then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());

    }

    @Test
    @DisplayName("?????? ???????????? ?????? ?????? - ?????? ?????? ?????? ??????")
    void saveFailedCancelTransaction() {
        // given
        AccountUser harry = AccountUser.builder()
                .name("harry").build();
        harry.setId(17L);

        Account account = Account.builder()
                .accountUser(harry)
                .accountNumber("1234567890")
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(CANCEL)
                        .transactionResult(F)
                        .amount(1000L)
                        .balanceSnapshot(10000L)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        transactionService.saveFailedCancelTransaction("1234567890", CANCEL_AMOUNT);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(CANCEL_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(F, captor.getValue().getTransactionResult());
        assertEquals(CANCEL, captor.getValue().getTransactionType());

    }

    @Test
    @DisplayName("?????? ?????? ??????")
    void queryTransactionSuccess() {
        // given
        AccountUser harry = AccountUser.builder()
                .name("harry").build();
        harry.setId(17L);

        Account account = Account.builder()
                .accountUser(harry)
                .accountNumber("1234567890")
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(CANCEL)
                .transactionResult(F)
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(10000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        // when
        TransactionDto transactionDto = transactionService.queryTransaction("12345");

        // then
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(F, transactionDto.getTransactionResult());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());

    }

    @Test
    @DisplayName("????????? ?????? - ?????? ?????? ?????? ??????")
    void queryTransactionSuccess_TransactionNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        // then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

}