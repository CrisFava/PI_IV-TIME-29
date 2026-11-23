// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.domain.model.user;

import com.converge.api.modules.user.domain.enums.user.Badge;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PaymentStanding;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverLicense;
import com.converge.api.modules.user.domain.model.user.subtypes.Reputation;
import com.converge.api.modules.user.domain.model.user.subtypes.ReputationWindowCounts;
import com.converge.api.modules.user.domain.model.user.subtypes.UserBadge;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.*;

import static com.converge.api.modules.user.domain.enums.user.ParticipationRole.DRIVER;
import static com.converge.api.modules.user.domain.enums.user.ParticipationRole.PASSENGER;

/**
 * Entidade de usuário (coleção "users" no MongoDB).
 *
 * Guarda os dados de perfil (nome, e-mail, telefone, foto), os papéis de
 * participação (passageiro e/ou motorista), o vínculo acadêmico, a CNH, a chave
 * PIX, a situação de pagamento e inadimplência, a reputação e os selos.
 * Concentra as regras de domínio do usuário: o que é preciso para ser motorista,
 * quando o usuário está bloqueado por dívida, como a reputação é atualizada e
 * quando o selo de confiança é concedido ou perdido.
 *
 * O e-mail institucional é único (índice parcial: só vale quando preenchido),
 * assim como o e-mail e o firebaseUid. A igualdade entre usuários considera só
 * id e firebaseUid.
 */
@Document(collection = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@CompoundIndex(
        name = "unique_institutional_email",
        def = "{'academicRecord.institutionalEmail': 1}",
        unique = true,
        partialFilter = "{'academicRecord.institutionalEmail': {$type: 'string'}}"
)
public class User {
    @Id
    @EqualsAndHashCode.Include
    private ObjectId id;

    @NotBlank
    @Indexed(unique = true)
    @EqualsAndHashCode.Include
    private String firebaseUid;

    @NotBlank
    @Size(min = 3)
    private String name;

    @NotBlank
    @Email
    @Setter
    @Indexed(unique = true)
    private String email;

    @NotBlank
    @Setter
    private String phone;

    @Nullable
    @Setter
    private String photoUrl;

    // Sem final: o Spring Data não preenche campo final ao ler do Mongo, e os papéis voltavam vazios.
    @NotEmpty
    private Set<ParticipationRole> participationRoles = new HashSet<>();

    @NotNull
    @Valid
    private AcademicRecord academicRecord;

    @Nullable
    @Valid
    @Setter
    private DriverLicense driverLicense;

    @Nullable
    @Setter
    private String pixKey;

    @Nullable
    @Setter
    private PixKeyType pixKeyType;

    @NotNull
    @Setter
    private PaymentStanding paymentStanding = PaymentStanding.UP_TO_DATE;

    @PositiveOrZero
    @Setter
    private short overdueRidesCount = 0;

    @Setter
    @Field("isDefaulter")
    @Indexed
    private boolean isDefaulter = false;

    @Setter
    private LocalDateTime defaulterSince;

    @Valid
    @Setter
    private Reputation reputation = Reputation.defaultInitial();

    // Sem final pelo mesmo motivo de participationRoles.
    private Set<@Valid UserBadge> badges = new HashSet<>();

    @Setter
    private boolean active = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Construtor completo, usado no cadastro. Inicializa os papéis (sempre com
     * PASSENGER), aplica a chave PIX e guarda os dados informados. Se o usuário
     * pediu o papel DRIVER, valida na hora se ele tem CNH e PIX, senão lança
     * BAD_REQUEST.
     */
    public User(
            String firebaseUid,
            String name,
            String email,
            String phone,
            @Nullable String photoUrl,
            Set<ParticipationRole> participationRoles,
            AcademicRecord academicRecord,
            @Nullable DriverLicense driverLicense,

            @Nullable String pixKey,

            @Nullable PixKeyType pixKeyType

    ) {
        this.initRoleSet(participationRoles);

        this.firebaseUid = firebaseUid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.photoUrl = photoUrl;
        this.academicRecord = academicRecord;

        this.applyPixKey(pixKey, pixKeyType);
        this.driverLicense = driverLicense;

        if (participationRoles.contains(DRIVER)){
            this.validateDriverRequirements();
        }
    }

    public User(
            String firebaseUid,
            String name,
            String email,
            String phone,
            Set<ParticipationRole> participationRoles,
            AcademicRecord academicRecord
    ) {
        this(
                firebaseUid,
                name,
                email,
                phone,
                null,
                participationRoles,
                academicRecord,
                null,
                null,
                null);
    }

    /**
     * Adiciona um selo à lista. Compara pelo record inteiro (tipo + data), então
     * prefira grantBadge, que evita duplicar o mesmo tipo.
     */
    public void addBadge(UserBadge badge) {
        this.badges.add(badge);
    }


    /**
     * Remove um selo pelo record inteiro (tipo + data). Para remover por tipo,
     * use revokeBadge.
     */
    public void removeBadge(UserBadge badge) {
        this.badges.remove(badge);
    }

    /**
     * Diz se o usuário tem exatamente aquele selo (tipo + data). Para saber se ele
     * tem o tipo, use hasBadgeOfType.
     */
    public boolean hasBadge(UserBadge badge) {
        return this.badges.contains(badge);
    }

    // Os selos são conferidos pelo tipo: o UserBadge também carrega a data, então a igualdade do record não serve.
    public boolean hasBadgeOfType(Badge type) {
        return this.badges.stream().anyMatch(b -> b.badge() == type);
    }

    // Concede o selo só se o usuário ainda não o tem, preservando a data da concessão original.
    public boolean grantBadge(Badge type, LocalDateTime now) {
        if (hasBadgeOfType(type)) {
            return false;
        }
        this.badges.add(new UserBadge(type, now));
        return true;
    }

    public boolean revokeBadge(Badge type) {
        return this.badges.removeIf(b -> b.badge() == type);
    }

    // Selo de confiança (RF69): cumpre os critérios em algum papel e não estourou os limites de penalidade.
    public boolean qualifiesForTrustBadge() {
        return !trustThresholdExceeded();
    }

    public boolean hasRole(ParticipationRole role) {
        // Todo usuário é passageiro, mesmo que o papel tenha se perdido no banco
        return role == PASSENGER || this.participationRoles.contains(role);
    }

    public boolean isAcademicVerified() {
        return this.academicRecord != null && this.academicRecord.status() == VerificationStatus.VERIFIED;
    }

    // RF67: bloqueado pela situação de pagamento ou marcado como inadimplente
    public boolean isBlockedByDebt() {
        return this.paymentStanding == PaymentStanding.BLOCKED || this.isDefaulter;
    }

    // RF72: o motorista só oferece carona com a chave PIX cadastrada
    public boolean hasPixKey() {
        return this.pixKey != null && !this.pixKey.isBlank();
    }

    public boolean hasValidDriverLicense(LocalDateTime now) {
        return this.driverLicense != null
                && this.driverLicense.effectiveStatus(now) == VerificationStatus.VERIFIED;
    }

    /**
     * Adiciona um papel ao usuário. Para DRIVER, exige antes CNH e PIX
     * configurados (lança BAD_REQUEST se faltar). Garante que PASSENGER também
     * esteja na lista. Ignora papel nulo.
     */
    public void addRole(ParticipationRole role) {
        if (role == null) return;
        if (role == DRIVER){
            this.validateDriverRequirements();
        }

        this.participationRoles.add(PASSENGER);
        this.participationRoles.add(role);
    }


    /**
     * Remove um papel do usuário. O papel PASSENGER não pode ser removido.
     */
    public void removeRole(ParticipationRole role) {
        if (role == PASSENGER) return;
        this.participationRoles.remove(role);
    }


    /**
     * Devolve os papéis do usuário como conjunto somente leitura, sempre
     * incluindo PASSENGER.
     */
    public Set<ParticipationRole> getParticipationRoles() {
        Set<ParticipationRole> roles = new HashSet<>(this.participationRoles);
        roles.add(PASSENGER);
        return Collections.unmodifiableSet(roles);
    }

    /**
     * Devolve os selos do usuário como conjunto somente leitura (a lista só muda
     * pelos métodos da própria entidade).
     */
    public Set<UserBadge> getBadges() {
        return Collections.unmodifiableSet(this.badges);
    }


    /**
     * Inicializa o conjunto de papéis: sempre com PASSENGER, mais os papéis
     * informados (se houver).
     */
    private void initRoleSet(@Nullable Set<ParticipationRole> roles){
        this.participationRoles.add(PASSENGER);

        if (roles != null){
            this.participationRoles.addAll(roles);
        }
    }

    /**
     * Confere os requisitos para ser motorista: chave PIX com o tipo e CNH
     * cadastrada. Se faltar algo, lança BAD_REQUEST listando os campos que faltam.
     */
    private void validateDriverRequirements(){

        List<String> fields = new ArrayList<>(2);

        if (this.pixKey == null || this.pixKey.isBlank() || this.pixKeyType == null) {
            fields.addAll(List.of("pixKey", "pixKeyType"));
        }

        if (this.driverLicense == null) {
            fields.add("driverLicense");
        }

        if (!fields.isEmpty()){
            String message = "Driver requirements not met. Ensure the driver has a " +
                    "valid driver's license and properly configured PIX information.";

            throw new ApplicationException(ErrorCode.BAD_REQUEST, message, Set.copyOf(fields));
        }
    }

    /**
     * Aplica a chave PIX e o tipo, que andam juntos. Sem nenhum dos dois, limpa
     * ambos; com só um dos dois, lança BAD_REQUEST. Quando vêm os dois, a chave
     * é normalizada conforme o tipo (pixKeyType.normalize) antes de guardar.
     */
    private void applyPixKey(@Nullable String pixKey, @Nullable PixKeyType pixKeyType) {
        boolean hasKey = pixKey != null && !pixKey.isBlank();

        if (!hasKey && pixKeyType == null) {
            this.pixKey = null;
            this.pixKeyType = null;
            return;
        }

        if (!hasKey || pixKeyType == null) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Informe a chave PIX e o tipo da chave.",
                    Set.of("pixKey", "pixKeyType")
            );
        }

        this.pixKey = pixKeyType.normalize(pixKey);
        this.pixKeyType = pixKeyType;
    }


    /**
     * Atualiza os dados de perfil (nome, telefone, foto e PIX). Se o usuário é
     * motorista, revalida os requisitos, o que impede remover o PIX de quem dirige.
     */
    public void updateProfile(
            String name,
            String phone,
            @Nullable String photoUrl,
            @Nullable String pixKey,
            @Nullable PixKeyType pixKeyType
    ) {
        this.name = name;
        this.phone = phone;
        this.photoUrl = photoUrl;
        this.applyPixKey(pixKey, pixKeyType);

        if (participationRoles.contains(DRIVER)){
            this.validateDriverRequirements();
        }
    }

    /**
     * Registra a CNH enviada pelo usuário, substituindo a anterior. Lança
     * BAD_REQUEST se vier nula.
     */
    public void submitDriverLicense(DriverLicense driverLicense) {
        if (driverLicense == null) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Informe os dados da CNH.",
                    Set.of("driverLicense")
            );
        }
        this.driverLicense = driverLicense;
    }


    /**
     * Desativa a conta (exclusão lógica, os dados continuam salvos).
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Registra um cancelamento feito pelo motorista na reputação e, se isso
     * estourar o limite, revoga o selo de confiança.
     */
    public void recordDriverCancellation(LocalDateTime now) {
        this.reputation = currentReputation().withDriverCancellation(now);
        revokeTrustBadgeIfThresholdExceeded();
    }


    /**
     * Registra um cancelamento tardio de passageiro na reputação e revoga o
     * selo de confiança se o limite for estourado.
     */
    public void recordPassengerLateCancellation(LocalDateTime now) {
        this.reputation = currentReputation().withPassengerLateCancellation(now);
        revokeTrustBadgeIfThresholdExceeded();
    }

    /**
     * Registra uma ausência (no-show) de passageiro na reputação e revoga o
     * selo de confiança se o limite for estourado.
     */
    public void recordPassengerNoShow(LocalDateTime now) {
        this.reputation = currentReputation().withPassengerNoShow(now);
        revokeTrustBadgeIfThresholdExceeded();
    }

    /**
     * Registra uma carona com pagamento vencido de passageiro na reputação e
     * revoga o selo de confiança se o limite for estourado.
     */
    public void recordPassengerOverdueRide(LocalDateTime now) {
        this.reputation = currentReputation().withPassengerOverdueRide(now);
        revokeTrustBadgeIfThresholdExceeded();
    }

    /**
     * Registra uma carona concluída como motorista na reputação. Não mexe nos selos.
     */
    public void recordDriverCompletedRide(LocalDateTime now) {
        this.reputation = currentReputation().withDriverCompletedRide(now);
    }

    /**
     * Registra uma carona concluída como passageiro na reputação. Não mexe nos selos.
     */
    public void recordPassengerCompletedRide(LocalDateTime now) {
        this.reputation = currentReputation().withPassengerCompletedRide(now);
    }
    // Troca os contadores da janela pelos recalculados a partir das viagens.
    /**
     * Substitui os contadores da janela de reputação pelos valores recalculados
     * a partir do histórico de viagens (usado por rotina de recálculo).
     */
    public void refreshReputationWindow(ReputationWindowCounts counts, LocalDateTime now) {
        this.reputation = currentReputation().withWindowCounts(counts, now);
    }

    /**
     * Atualiza a nota média e o total de avaliações do usuário como motorista.
     */
    public void recordDriverRating(double averageRating, int totalReviews, LocalDateTime now) {
        this.reputation = currentReputation().withDriverRating(averageRating, totalReviews, now);
    }

    /**
     * Atualiza a nota média e o total de avaliações do usuário como passageiro.
     */
    public void recordPassengerRating(double averageRating, int totalReviews, LocalDateTime now) {
        this.reputation = currentReputation().withPassengerRating(averageRating, totalReviews, now);
    }

    /**
     * Devolve a reputação atual, ou uma reputação inicial padrão se o usuário
     * ainda não tiver (evita nulo em registros antigos).
     */
    private Reputation currentReputation() {
        return this.reputation != null ? this.reputation : Reputation.defaultInitial();
    }

    /**
     * Remove o selo de confiança se o usuário passou dos limites de penalidade.
     */
    private void revokeTrustBadgeIfThresholdExceeded() {
        if (trustThresholdExceeded()) {
            this.badges.removeIf(b -> b.badge() == Badge.TRUST);
        }
    }


    /**
     * Decide se o usuário perdeu (ou não merece) o selo de confiança. Retorna
     * true quando ocorre qualquer um destes casos:
     * - motorista com mais de 2 cancelamentos iniciados por ele na janela;
     * - passageiro com mais de 2 cancelamentos tardios, mais de 1 no-show ou
     *   qualquer carona com pagamento vencido na janela;
     * - não cumpre os critérios de confiança nem como motorista nem como
     *   passageiro (basta cumprir em um dos papéis).
     * Contadores nulos são tratados como "sem violação".
     */
    private boolean trustThresholdExceeded() {
        Reputation current = currentReputation();
        boolean driverEligible = current.driver() != null && current.driver().meetsTrustCriteria();
        boolean passengerEligible = current.passenger() != null && current.passenger().meetsTrustCriteria();
        boolean driverViolated = current.driver() != null
                && current.driver().window() != null
                && current.driver().window().driverInitiatedCancellations() != null
                && current.driver().window().driverInitiatedCancellations() > 2;
        boolean passengerViolated = current.passenger() != null
                && current.passenger().window() != null
                && ((current.passenger().window().lateCancellations() != null
                && current.passenger().window().lateCancellations() > 2)
                || (current.passenger().window().noShows() != null
                && current.passenger().window().noShows() > 1)
                || (current.passenger().window().overdueRides() != null
                && current.passenger().window().overdueRides() > 0));

        return driverViolated || passengerViolated || (!driverEligible && !passengerEligible);
    }
}
