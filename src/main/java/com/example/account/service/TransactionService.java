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
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    /**
     * 잔액 사용
     * 사용자 없는 경우, 사용자 아이디와 계좌 소유주가 다른 경우,
     * 계좌가 이미 해지 상태인 경우, 거래금액이 잔액보다 큰 경우,
     * 거래금액이 너무 작거나 큰 경우 실패 응답
     * - 해당 계좌에서 거래(사용, 사용 취소)가 진행 중일 때
     * 다른 거래 요청이 오는 경우 해당 거래가 동시에 잘못 처리되는 것을 방지해야 한다.
     * @param userId
     * @param accountNumber
     * @param amount
     * @return 계좌번호, 거래 결과 코드(성공/실패), 거래 아이디, 거래금액, 거래일시
     */
    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber, Long amount) {

        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateUseBalance(accountUser, account, amount);

        /* 중요한 데이터를 변경할 때에는 domain내부에서 예외처리와 함께 처리
        Long accountBalance = account.getBalance();
        account.setBalance(accountBalance - amount);*/
        account.useBalance(amount);

        Transaction transaction = saveAndGetTransaction(USE, S, account, amount);

        return TransactionDto.fromEntity(transaction);
    }

    private static void validateUseBalance(AccountUser accountUser, Account account, Long amount) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }

        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }

        if (account.getBalance() < amount) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }
    }

    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(USE, F, account, amount);

    }

    /**
     * 잔액 사용 취소
     * 거래 아이디에 해당하는 거래가 없는 경우, 계좌가 없는 경우, 거래와 계좌가 일치하지 않는 경우
     * 거래금액과 거래 취소 금액이 다른경우(부분 취소 불가능) 실패 응답
     * - 1년이 넘은 거래는 사용 취소 불가능
     * - 해당 계좌에서 거래(사용, 사용 취소)가 진행 중일 때
     * 다른 거래 요청이 오는 경우 해당 거래가 동시에 잘못 처리되는 것을 방지해야 한다.
     * @param transactionId
     * @param accountNumber
     * @param amount
     * @return 계좌번호, 거래 결과 코드(성공/실패), 거래 아이디, 거래금액, 거래일
     */
    @Transactional
    public TransactionDto cancelBalance(String transactionId, String accountNumber, Long amount) {

        Transaction useTransaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateCancelBalance(useTransaction, account, amount);

        account.cancelBalance(amount);

        Transaction cancelTransaction = saveAndGetTransaction(CANCEL, S, account, amount);

        return TransactionDto.fromEntity(cancelTransaction);
    }

    private static void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        if (!Objects.equals(transaction.getAccount().getId(), account.getId())) {
            throw new AccountException(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH);
        }

        if (!Objects.equals(transaction.getAmount(), amount)) {
            throw new AccountException(ErrorCode.TRANSACTION_AMOUNT_UN_MATCH);
        }

        if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            throw new AccountException(ErrorCode.TOO_OLD_ORDER_TO_CANCEL);
        }
    }

    @Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(CANCEL, F, account, amount);
    }

    private Transaction saveAndGetTransaction(TransactionType transactionType,
                                              TransactionResultType transactionResultType, Account account, Long amount) {
        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResult(transactionResultType)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        /*UUID를 사용하여 임의의 ID를 받는다.*/
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }

    @Transactional
    public TransactionDto queryTransaction(String transactionId) {
        return TransactionDto.fromEntity(
                transactionRepository.findByTransactionId(transactionId)
                        .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND))
        );
    }
}
