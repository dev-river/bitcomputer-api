package com.bitcomputer.portal.bgc;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NameSplitterTest {

    @Test
    void simpleName_splitsFirstCharacterAsLastName() {
        NameSplitter.SplitName result = NameSplitter.split("김민준");
        assertThat(result.lastName()).isEqualTo("김");
        assertThat(result.firstName()).isEqualTo("민준");
    }

    @Test
    void compoundSurname_namgung_splitsTwoCharacters() {
        NameSplitter.SplitName result = NameSplitter.split("남궁서준");
        assertThat(result.lastName()).isEqualTo("남궁");
        assertThat(result.firstName()).isEqualTo("서준");
    }

    @Test
    void compoundSurname_hwangbo_splitsTwoCharacters() {
        NameSplitter.SplitName result = NameSplitter.split("황보라온");
        assertThat(result.lastName()).isEqualTo("황보");
        assertThat(result.firstName()).isEqualTo("라온");
    }

    @Test
    void compoundSurname_seonwoo_splitsTwoCharacters() {
        NameSplitter.SplitName result = NameSplitter.split("선우진");
        assertThat(result.lastName()).isEqualTo("선우");
        assertThat(result.firstName()).isEqualTo("진");
    }

    @Test
    void blankName_throws() {
        assertThrows(IllegalArgumentException.class, () -> NameSplitter.split("  "));
    }

    @Test
    void singleCharacterName_throws() {
        assertThrows(IllegalArgumentException.class, () -> NameSplitter.split("김"));
    }

    @Test
    void bareCompoundSurnameWithNoGivenName_throws() {
        assertThrows(IllegalArgumentException.class, () -> NameSplitter.split("남궁"));
    }
}
