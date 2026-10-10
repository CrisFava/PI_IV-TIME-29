package com.converge.api.modules.user.domain.model.user;

import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PaymentStanding;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.DriverLicense;
import com.converge.api.modules.user.domain.model.user.subtypes.Reputation;
import com.converge.api.modules.user.domain.model.user.subtypes.UserBadge;
import com.mongodb.lang.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
@NoArgsConstructor
@Getter
@EqualsAndHashCode
@CompoundIndex(
        name = "unique_institutional_email",
        def = "{'academicRecord.institutionalEmail': 1}",
        unique = true,
        partialFilter = "{'academicRecord.institutionalEmail': {$type: 'string'}}"
)
public class User {
    @Id
    private ObjectId id;

    @NotBlank
    @Indexed(unique = true)
    private String firebaseUid;

    @NotBlank
    @Size(min = 3)
    private String name;

    @NotBlank
    @Email
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
    private Reputation reputation;

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
            @Nullable
            String photoUrl,
            Set<ParticipationRole> participationRoles,
            AcademicRecord academicRecord,
            @Nullable
            DriverLicense driverLicense,

            @Nullable
            String pixKey
    ) {
        this.firebaseUid = firebaseUid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.photoUrl = photoUrl;
        if (participationRoles != null) {
            this.participationRoles.addAll(participationRoles);
        }
        this.academicRecord = academicRecord;
        this.driverLicense = driverLicense;
        this.pixKey = pixKey;
        this.overdueRidesCount = 0;
        this.isDefaulter = false;
        this.reputation = Reputation.defaultInitial();
        this.active = true;
    }

    public User(
            String firebaseUid,
            String name,
            String email,
            String phone,
            Set<ParticipationRole> participationRoles,
            AcademicRecord academicRecord
    ) {
        this(firebaseUid,
                name,
                email,
                phone,
                null,
                participationRoles,
                academicRecord,
                null,
                null);
    }


    public void setEmail(@Email String email){
        this.email = email;
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
        this.participationRoles.add(role);
    }

    public void removeRole(ParticipationRole role) {
        this.participationRoles.remove(role);
    }
}
