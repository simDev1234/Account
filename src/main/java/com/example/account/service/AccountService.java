package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.dto.AccountInfo;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.account.type.AccountStatus.IN_USE;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository; // final 의 경우 생성자에서만 초기화될 수 있다.
    private final AccountUserRepository accountUserRepository;

    /**
     * 계좌 생성
     * 사용자가 있는지 조회
     * 사용자의 계좌가 10개인지 확인
     * 계좌의 번호를 생성
     * 계좌를 저장하고, 그 정보를 넘긴다.
     *
     * @param userId
     * @param initialBalance
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {

        AccountUser accountUser = getAccountUser(userId);

        // 비즈니스 로직상의 validation.
        // -- 너무 많은 예외처리는 좋지 않다. (별도로 처리하는 게 좋다.)
        validateCreateAccount(accountUser);

        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> (Integer.parseInt(account.getAccountNumber()) + 1) + "")
                .orElse("1000000000");

        return AccountDto.fromEntity(
                accountRepository.save(
                        Account.builder()
                                .accountUser(accountUser)
                                .accountNumber(newAccountNumber)
                                .accountStatus(IN_USE)
                                .balance(initialBalance)
                                .registeredAt(LocalDateTime.now())
                                .build()
                )
        );

    }

    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countByAccountUser(accountUser) == 10) {
            throw new AccountException(ErrorCode.MAX_ACCOUNT_PER_USER_10);
        }
    }

    /**
     * 계좌 해지
     * 사용자 또는 계좌가 없는 경우,
     * 사용자 아이디와 계좌 소유주 일치하지 않는 경우,
     * 계좌가 이미 해지 상태인 경우,
     * 잔액이 있는 경우 실패 응답
     * 계좌 상태 수정 (IN_USER -> UNREGISTRERED)
     * 계좌 해지 일시 수정 (null -> now())
     * @param userId
     * @param accountNumber
     * @return AccountDto(userId, accountNumber, unregisteredAt)
     */
    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {

        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnregisteredAt(LocalDateTime.now());

        accountRepository.save(account); // 없어도 작동한다.(테스트용 / 비추)

        return AccountDto.fromEntity(account);
    }

    private static void validateDeleteAccount(AccountUser accountUser, Account account) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }

        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }

        if (account.getBalance() > 0) {
            throw new AccountException(ErrorCode.BALANCE_NOT_EMPTY);
        }

    }

    /**
     * 계좌 확인
     * 사용자가 없는 경우 실패 응답
     * @param userId
     * @return List<userId, accountNumber>
     */
    @Transactional
    public List<AccountDto> getAccountByUserId(Long userId) {

        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        List<Account> accounts = accountRepository.findByAccountUser(accountUser);

        return accounts.stream()
                .map(AccountDto::fromEntity)
                .collect(Collectors.toList());
    }

    private AccountUser getAccountUser(Long userId) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        return accountUser;
    }

}
