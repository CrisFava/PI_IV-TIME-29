// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.domain.enums.user;

import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PixKeyTypeTest {

    @ParameterizedTest(name = "CPF \"{0}\" vira {1}")
    @DisplayName("CPF válido é normalizado para só dígitos, com ou sem máscara")
    @CsvSource({
            "529.982.247-25, 52998224725",
            "52998224725, 52998224725",
            "'529 982 247 25', 52998224725",
            "111.444.777-35, 11144477735",
            "'  529.982.247-25  ', 52998224725"
    })
    void shouldNormalizeValidCpf(String typed, String expected) {
        assertThat(PixKeyType.CPF.normalize(typed)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "CPF inválido: \"{0}\"")
    @DisplayName("CPF com dígito verificador errado, repetido, curto ou com letras é recusado")
    @ValueSource(strings = {
            "529.982.247-26",
            "529.982.248-25",
            "111.111.111-11",
            "000.000.000-00",
            "5299822472",
            "529.982.247-255",
            "abc.def.ghi-jk",
            ""
    })
    void shouldRejectInvalidCpf(String typed) {
        assertThrows(ApplicationException.class, () -> PixKeyType.CPF.normalize(typed));
    }

    @ParameterizedTest(name = "telefone \"{0}\" vira {1}")
    @DisplayName("Celular com DDD é normalizado para +55DDDNÚMERO, com ou sem máscara")
    @CsvSource({
            "'(19) 91234-5678', +5519912345678",
            "19912345678, +5519912345678",
            "'+55 19 91234-5678', +5519912345678",
            "5519912345678, +5519912345678",
            "'(11) 99999-0000', +5511999990000"
    })
    void shouldNormalizeValidPhone(String typed, String expected) {
        assertThat(PixKeyType.PHONE.normalize(typed)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "telefone inválido: \"{0}\"")
    @DisplayName("Telefone fixo, sem DDD, curto ou com DDD inválido é recusado")
    @ValueSource(strings = {
            "(19) 1234-5678",
            "912345678",
            "12345",
            "(00) 91234-5678",
            "(19) 81234-5678",
            ""
    })
    void shouldRejectInvalidPhone(String typed) {
        assertThrows(ApplicationException.class, () -> PixKeyType.PHONE.normalize(typed));
    }

    @Test
    @DisplayName("E-mail é normalizado para minúsculas e sem espaços nas pontas")
    void shouldNormalizeEmail() {
        assertThat(PixKeyType.EMAIL.normalize("  Ana.Souza@Email.COM ")).isEqualTo("ana.souza@email.com");
    }

    @ParameterizedTest(name = "e-mail inválido: \"{0}\"")
    @DisplayName("E-mail sem arroba, sem domínio completo ou em branco é recusado")
    @ValueSource(strings = {"ana.email.com", "ana@email", "@email.com", "ana@@email.com", ""})
    void shouldRejectInvalidEmail(String typed) {
        assertThrows(ApplicationException.class, () -> PixKeyType.EMAIL.normalize(typed));
    }

    @Test
    @DisplayName("Chave aleatória (UUID) é normalizada para minúsculas")
    void shouldNormalizeRandomKey() {
        assertThat(PixKeyType.RANDOM.normalize("123E4567-E89B-12D3-A456-426614174000"))
                .isEqualTo("123e4567-e89b-12d3-a456-426614174000");
    }

    @ParameterizedTest(name = "chave aleatória inválida: \"{0}\"")
    @DisplayName("Chave aleatória fora do formato UUID é recusada")
    @ValueSource(strings = {"123", "123e4567e89b12d3a456426614174000", "zzze4567-e89b-12d3-a456-426614174000", ""})
    void shouldRejectInvalidRandomKey(String typed) {
        assertThrows(ApplicationException.class, () -> PixKeyType.RANDOM.normalize(typed));
    }

    @ParameterizedTest(name = "tipo {0}")
    @DisplayName("Pix Copia e Cola colado no lugar da chave é recusado em qualquer tipo, com mensagem própria")
    @EnumSource(PixKeyType.class)
    void shouldRejectPixCopyAndPastePayload(PixKeyType type) {
        String payload = "00020126580014br.gov.bcb.pix0136123e4567-e89b-12d3-a456-4266141740005204000053039865802BR";

        ApplicationException thrown = assertThrows(ApplicationException.class, () -> type.normalize(payload));

        assertThat(thrown.getError().message()).contains("Copia e Cola");
    }

    @ParameterizedTest(name = "tipo {0}")
    @DisplayName("Chave nula é recusada em qualquer tipo")
    @EnumSource(PixKeyType.class)
    void shouldRejectNullKey(PixKeyType type) {
        assertThrows(ApplicationException.class, () -> type.normalize(null));
    }

    @Test
    @DisplayName("Tipo e conteúdo que não combinam são recusados")
    void shouldRejectKeyThatDoesNotMatchTheType() {
        assertThrows(ApplicationException.class, () -> PixKeyType.CPF.normalize("ana@email.com"));
        assertThrows(ApplicationException.class, () -> PixKeyType.EMAIL.normalize("529.982.247-25"));
        assertThrows(ApplicationException.class, () -> PixKeyType.PHONE.normalize("111.444.777-35"));
    }

    @Test
    @DisplayName("Onze dígitos que servem como CPF e como celular: quem decide é o tipo escolhido")
    void shouldLetTheTypeDecideWhenDigitsFitTwoFormats() {
        assertThat(PixKeyType.CPF.normalize("529.982.247-25")).isEqualTo("52998224725");
        assertThat(PixKeyType.PHONE.normalize("529.982.247-25")).isEqualTo("+5552998224725");
    }

    @Test
    @DisplayName("O erro é 400 e aponta o campo pixKey")
    void shouldReportBadRequestOnThePixKeyField() {
        ApplicationException thrown = assertThrows(ApplicationException.class, () -> PixKeyType.CPF.normalize("123"));

        assertThat(thrown.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(thrown.getError().fields()).containsExactly("pixKey");
    }
}
