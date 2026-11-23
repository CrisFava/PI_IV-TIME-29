// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.config.mongo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Configuração do MongoDB para a aplicação.
 *
 * Faz duas coisas: liga a auditoria automática (@EnableMongoAuditing), que
 * preenche os campos @CreatedDate e @LastModifiedDate das entidades (como
 * createdAt e updatedAt em User e Vehicle), e liga o suporte a transações
 * (@EnableTransactionManagement), registrando o gerenciador que permite usar
 * @Transactional com o Mongo.
 */
@Configuration
@EnableMongoAuditing
@EnableTransactionManagement
public class MongoConfig {

    /**
     * Cria o gerenciador de transações do MongoDB a partir da conexão com o banco.
     * É ele que o Spring usa quando um método está anotado com @Transactional,
     * garantindo que várias operações sejam confirmadas juntas ou desfeitas juntas.
     */
    @Bean
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }
}
