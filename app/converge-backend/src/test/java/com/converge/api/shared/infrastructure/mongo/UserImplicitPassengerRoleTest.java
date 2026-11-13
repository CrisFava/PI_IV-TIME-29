// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.shared.infrastructure.mongo;

import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.model.user.User;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enquanto as coleções eram final, o banco foi sobrescrito e alguns usuários
 * perderam o papel de passageiro. Todo usuário continua passageiro mesmo assim.
 */
class UserImplicitPassengerRoleTest {

    private MappingMongoConverter converter;

    @BeforeEach
    void setUp() {
        MongoCustomConversions conversions = new MongoCustomConversions(List.of());
        MongoMappingContext context = new MongoMappingContext();
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        context.afterPropertiesSet();
        converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
    }

    @Test
    @DisplayName("User: quem perdeu o papel de passageiro no banco continua passageiro")
    void shouldKeepPassengerRoleImplicit() {
        User user = converter.read(User.class, new Document("firebaseUid", "uid-1")
                .append("participationRoles", List.of("DRIVER")));

        assertThat(user.hasRole(ParticipationRole.PASSENGER)).isTrue();
        assertThat(user.getParticipationRoles()).contains(ParticipationRole.PASSENGER);
    }
}
