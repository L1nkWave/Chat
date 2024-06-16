package org.linkwave.shared.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Headers {

    TOTAL_COUNT("X-Total-Count");

    private final String value;

}
