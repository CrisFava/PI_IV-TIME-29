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

    public void addBadge(UserBadge badge) {
        this.badges.add(badge);
    }

    public void removeBadge(UserBadge badge) {
        this.badges.remove(badge);
    }

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

    public void addRole(ParticipationRole role) {
        if (role == null) return;
        if (role == DRIVER){
            this.validateDriverRequirements();
        }

        this.participationRoles.add(PASSENGER);
        this.participationRoles.add(role);
    }

    public void removeRole(ParticipationRole role) {
        if (role == PASSENGER) return;
        this.participationRoles.remove(role);
    }

    public Set<ParticipationRole> getParticipationRoles() {
        Set<ParticipationRole> roles = new HashSet<>(this.participationRoles);
        roles.add(PASSENGER);
        return Collections.unmodifiableSet(roles);
    }

    public Set<UserBadge> getBadges() {
        return Collections.unmodifiableSet(this.badges);
    }

    private void initRoleSet(@Nullable Set<ParticipationRole> roles){
        this.participationRoles.add(PASSENGER);

        if (roles != null){
            this.participationRoles.addAll(roles);
        }
    }

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

    public void deactivate() {
        this.active = false;
    }

    public void recordDriverCancellation(LocalDateTime now) {
        this.reputation = currentReputation().withDriverCancellation(now);
        revokeTrustBadgeIfThresholdExceeded();
    }

    public void recordPassengerLateCancellation(LocalDateTime now) {
        this.reputation = currentReputation().withPassengerLateCancellation(now);
        revokeTrustBadgeIfThresholdExceeded();
    }

    public void recordPassengerNoShow(LocalDateTime now) {
        this.reputation = currentReputation().withPassengerNoShow(now);
        revokeTrustBadgeIfThresholdExceeded();
    }

    public void recordPassengerOverdueRide(LocalDateTime now) {
        this.reputation = currentReputation().withPassengerOverdueRide(now);
        revokeTrustBadgeIfThresholdExceeded();
    }

    public void recordDriverCompletedRide(LocalDateTime now) {
        this.reputation = currentReputation().withDriverCompletedRide(now);
    }

    public void recordPassengerCompletedRide(LocalDateTime now) {
        this.reputation = currentReputation().withPassengerCompletedRide(now);
    }

    // Troca os contadores da janela pelos recalculados a partir das viagens. Não mexe nos selos.
    public void refreshReputationWindow(ReputationWindowCounts counts, LocalDateTime now) {
        this.reputation = currentReputation().withWindowCounts(counts, now);
    }

    public void recordDriverRating(double averageRating, int totalReviews, LocalDateTime now) {
        this.reputation = currentReputation().withDriverRating(averageRating, totalReviews, now);
    }

    public void recordPassengerRating(double averageRating, int totalReviews, LocalDateTime now) {
        this.reputation = currentReputation().withPassengerRating(averageRating, totalReviews, now);
    }

    private Reputation currentReputation() {
        return this.reputation != null ? this.reputation : Reputation.defaultInitial();
    }

    private void revokeTrustBadgeIfThresholdExceeded() {
        if (trustThresholdExceeded()) {
            this.badges.removeIf(b -> b.badge() == Badge.TRUST);
        }
    }

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
