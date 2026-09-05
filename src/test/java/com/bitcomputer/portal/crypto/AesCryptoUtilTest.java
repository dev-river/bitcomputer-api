package com.bitcomputer.portal.crypto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AesCryptoUtilTest {

    private final AesCryptoUtil util = new AesCryptoUtil("MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");

    @Test
    void encryptThenDecrypt_returnsOriginalValue() {
        String plain = "1990-03-15";
        String cipher = util.encrypt(plain);
        assertThat(cipher).isNotEqualTo(plain);
        assertThat(util.decrypt(cipher)).isEqualTo(plain);
    }

    @Test
    void encryptingSameValueTwice_producesDifferentCiphertext() {
        String plain = "김민준";
        assertThat(util.encrypt(plain)).isNotEqualTo(util.encrypt(plain));
    }

    @Test
    void nullInput_roundTripsAsNull() {
        assertThat(util.encrypt(null)).isNull();
        assertThat(util.decrypt(null)).isNull();
    }

    @Test
    void invalidKeyLength_throwsOnConstruction() {
        String shortKey = java.util.Base64.getEncoder().encodeToString("tooshort".getBytes());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
            () -> new AesCryptoUtil(shortKey));
    }
}
