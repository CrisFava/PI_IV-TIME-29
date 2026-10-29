package com.converge.api.modules.user.domain.model.user;

import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PaymentStanding;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverLicense;
import com.converge.api.modules.user.domain.model.user.subtypes.Reputation;
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

    @NotEmpty
    private final Set<ParticipationRole> participationRoles = new HashSet<>();

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

    private final Set<@Valid UserBadge> badges = new HashSet<>();

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

        this.pixKey = pixKey;
        this.pixKeyType = pixKeyType;
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

    public boolean hasRole(ParticipationRole role) {
        return this.participationRoles.contains(role);
    }

    public void addRole(ParticipationRole role) {
        if (role == null) return;
        if (role == DRIVER){
            this.validateDriverRequirements();
        }

        this.participationRoles.add(role);
    }

    public void removeRole(ParticipationRole role) {
        if (role == PASSENGER) return;
        this.participationRoles.remove(role);
    }

    public Set<ParticipationRole> getParticipationRoles() {
        return Collections.unmodifiableSet(this.participationRoles);
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

    public void updateProfile(

            String name,
            String phone,
            @Nullable String photoUrl,
            @Nullable DriverLicense driverLicense,
            @Nullable String pixKey,
            @Nullable PixKeyType pixKeyType
    ) {
        this.name = name;
        this.phone = phone;
        this.photoUrl = photoUrl;
        this.driverLicense = driverLicense;
        this.pixKey = pixKey;
        this.pixKeyType = pixKeyType;

        if (participationRoles.contains(DRIVER)){
            this.validateDriverRequirements();
        }
    }

    public void deactivate() {
        this.active = false;
    }
}
