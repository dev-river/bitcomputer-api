package com.bitcomputer.portal.crypto;

import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EncryptedStringTypeHandlerRegistrationTest {

    @Test
    void doesNotBecomeTheGlobalDefaultHandlerForString() {
        Configuration configuration = new Configuration();
        var registry = configuration.getTypeHandlerRegistry();
        var defaultStringHandlerBefore = registry.getTypeHandler(String.class);

        registry.register(new EncryptedStringTypeHandler(new AesCryptoUtil("MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")));

        assertThat(registry.getTypeHandler(String.class)).isSameAs(defaultStringHandlerBefore);
    }

    @Test
    void constructorInjectionWorks() throws Exception {
        var aesCryptoUtil = new AesCryptoUtil("MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
        var handler = new EncryptedStringTypeHandler(aesCryptoUtil);

        assertThat(handler).isNotNull();
    }
}
