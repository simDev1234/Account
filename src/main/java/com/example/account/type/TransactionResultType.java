package com.example.account.type;

import lombok.*;

@AllArgsConstructor
public enum TransactionResultType {
    S("성공"), F("실패");

    private String description;
}
