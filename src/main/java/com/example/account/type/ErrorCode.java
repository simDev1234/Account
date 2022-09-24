package com.example.account.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR("내부 서버 오류가 발생했습니다."),
    USER_NOT_FOUND("사용자가 없습니다."),
    USER_ACCOUNT_UN_MATCH("사용자와 계좌의 소유주가 다릅니다."),
    ACCOUNT_NOT_FOUND("계좌가 없습니다."),
    MAX_ACCOUNT_PER_USER_10("사용자 최대 계좌는 10개입니다."),

    ACCOUNT_ALREADY_UNREGISTERED("계좌가 이미 해지되었습니다."),
    BALANCE_NOT_EMPTY("잔액이 남아있습니다."),
    AMOUNT_EXCEED_BALANCE("거래 금액이 계좌 잔액보다 큽니다."),
    TRANSACTION_NOT_FOUND("해당 트랜잭션이 없습니다."),
    TRANSACTION_ACCOUNT_UN_MATCH("현재 계좌가 거래에 사용된 계좌와 일치하지 않습니다."),
    TRANSACTION_AMOUNT_UN_MATCH("거래 금액과 거래 취소 금액이 다릅니다.(부분 취소 불가능)"),
    TOO_OLD_ORDER_TO_CANCEL("1년이 넘은 거래는 사용 취소가 불가능합니다."),
    INVALID_REQUEST("잘못된 요청입니다.");

    private String description;

}
