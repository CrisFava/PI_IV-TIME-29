// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.application;

import com.converge.api.modules.indicator.application.dto.CompletedRideCommand;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.PlatformIndicator;
import com.converge.api.modules.indicator.domain.model.UserIndicator;
import com.converge.api.modules.indicator.domain.repository.PlatformIndicatorRepository;
import com.converge.api.modules.indicator.domain.repository.UserIndicatorRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegisterCompletedRideUseCaseTest {

    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 11, 12, 18, 30);
    private static final double CO2_KG_PER_LITER_GASOLINE = 2.31;

    @Mock
    private UserIndicatorRepository userIndicatorRepository;

    @Mock
    private PlatformIndicatorRepository platformIndicatorRepository;

    // Os mocks se comportam como um banco em memória, para a segunda viagem enxergar a primeira.
    private final Map<String, UserIndicator> users = new HashMap<>();
    private final Map<String, PlatformIndicator> platform = new HashMap<>();

    private RegisterCompletedRideUseCase registerCompletedRide;

    @BeforeEach
    void setUp() {
        registerCompletedRide = new RegisterCompletedRideUseCase(userIndicatorRepository, platformIndicatorRepository);

        when(userIndicatorRepository.findByUserIdAndPeriodAndReference(anyString(), any(), anyString()))
                .thenAnswer(call -> Optional.ofNullable(
                        users.get(key(call.getArgument(0), call.getArgument(1), call.getArgument(2)))));
        when(userIndicatorRepository.existsByUserId(anyString()))
                .thenAnswer(call -> users.values().stream()
                        .anyMatch(indicator -> indicator.getUserId().equals(call.getArgument(0))));
        when(userIndicatorRepository.save(any(UserIndicator.class))).thenAnswer(call -> {
            UserIndicator indicator = call.getArgument(0);
            users.put(key(indicator.getUserId(), indicator.getPeriod(), indicator.getReference()), indicator);
            return indicator;
        });

        when(platformIndicatorRepository.findByPeriodAndReference(any(), anyString()))
                .thenAnswer(call -> Optional.ofNullable(
                        platform.get(key("platform", call.getArgument(0), call.getArgument(1)))));
        when(platformIndicatorRepository.save(any(PlatformIndicator.class))).thenAnswer(call -> {
            PlatformIndicator indicator = call.getArgument(0);
            platform.put(key("platform", indicator.getPeriod(), indicator.getReference()), indicator);
            return indicator;
        });
    }

    private static String key(String owner, IndicatorPeriod period, String reference) {
        return owner + "|" + period + "|" + reference;
    }

    private CompletedRideCommand ride(String driverId, List<String> passengerIds, double distanceKm) {
        return new CompletedRideCommand("ride-1", driverId, passengerIds, distanceKm, "GASOLINE", 12.0, COMPLETED_AT);
    }

    private UserIndicator userIndicator(String userId, IndicatorPeriod period, String reference) {
        return users.get(key(userId, period, reference));
    }

    private PlatformIndicator platformIndicator(IndicatorPeriod period, String reference) {
        return platform.get(key("platform", period, reference));
    }

    @Test
    @DisplayName("Uma viagem deve entrar na semana, no mês, no semestre e no acumulado da plataforma")
    void shouldCountRideInEveryPlatformPeriod() {
        PlatformIndicator allTime = registerCompletedRide.execute(ride("driver", List.of("p1", "p2"), 24.0));

        // 2 carros evitados x 24 km / 12 km/l = 4 litros poupados
        double co2 = 4.0 * CO2_KG_PER_LITER_GASOLINE;

        assertThat(platform).hasSize(4);
        for (PlatformIndicator indicator : List.of(
                platformIndicator(IndicatorPeriod.WEEKLY, "2026-W46"),
                platformIndicator(IndicatorPeriod.MONTHLY, "2026-11"),
                platformIndicator(IndicatorPeriod.SEMESTERLY, "2026-S2"),
                platformIndicator(IndicatorPeriod.ALL_TIME, "ALL_TIME"))) {
            assertThat(indicator.getTotalRides()).isEqualTo(1);
            assertThat(indicator.getTotalSharedKm()).isEqualTo(24.0);
            assertThat(indicator.getTotalCarsAvoided()).isEqualTo(2);
            assertThat(indicator.getTotalCo2AvoidedKg()).isCloseTo(co2, within(1e-9));
            assertThat(indicator.getActiveUsers()).isEqualTo(3);
        }
        assertThat(allTime).isSameAs(platformIndicator(IndicatorPeriod.ALL_TIME, "ALL_TIME"));
    }

    @Test
    @DisplayName("Cada participante deve receber a viagem no seu papel e a sua fração do CO2")
    void shouldCountRideForEachParticipantInTheirRole() {
        registerCompletedRide.execute(ride("driver", List.of("p1", "p2"), 24.0));

        double co2Share = 4.0 * CO2_KG_PER_LITER_GASOLINE / 3;

        assertThat(users).hasSize(9);

        UserIndicator driver = userIndicator("driver", IndicatorPeriod.MONTHLY, "2026-11");
        assertThat(driver.getRidesAsDriver()).isEqualTo(1);
        assertThat(driver.getRidesAsPassenger()).isZero();
        assertThat(driver.getSharedKm()).isEqualTo(24.0);
        assertThat(driver.getCarsAvoided()).isEqualTo(2);
        assertThat(driver.getCo2AvoidedKg()).isCloseTo(co2Share, within(1e-9));

        UserIndicator passenger = userIndicator("p1", IndicatorPeriod.WEEKLY, "2026-W46");
        assertThat(passenger.getRidesAsDriver()).isZero();
        assertThat(passenger.getRidesAsPassenger()).isEqualTo(1);
        assertThat(passenger.getSharedKm()).isEqualTo(24.0);
        assertThat(passenger.getCarsAvoided()).isEqualTo(1);
        assertThat(passenger.getCo2AvoidedKg()).isCloseTo(co2Share, within(1e-9));

        assertThat(userIndicator("p2", IndicatorPeriod.SEMESTERLY, "2026-S2").getRidesAsPassenger()).isEqualTo(1);
    }

    @Test
    @DisplayName("A soma do CO2 dos participantes deve bater com o CO2 da plataforma")
    void shouldSplitPlatformCo2AmongParticipants() {
        registerCompletedRide.execute(ride("driver", List.of("p1", "p2"), 24.0));

        double sumOfShares = List.of("driver", "p1", "p2").stream()
                .mapToDouble(userId -> userIndicator(userId, IndicatorPeriod.MONTHLY, "2026-11").getCo2AvoidedKg())
                .sum();

        assertThat(sumOfShares).isCloseTo(
                platformIndicator(IndicatorPeriod.MONTHLY, "2026-11").getTotalCo2AvoidedKg(), within(1e-9));
    }

    @Test
    @DisplayName("Uma segunda viagem deve somar nos mesmos documentos e contar só os usuários novos")
    void shouldAccumulateSecondRideAndCountOnlyNewUsers() {
        registerCompletedRide.execute(ride("driver", List.of("p1", "p2"), 24.0));

        registerCompletedRide.execute(ride("driver", List.of("p1", "p3"), 10.0));

        PlatformIndicator allTime = platformIndicator(IndicatorPeriod.ALL_TIME, "ALL_TIME");
        assertThat(allTime.getTotalRides()).isEqualTo(2);
        assertThat(allTime.getTotalSharedKm()).isEqualTo(34.0);
        assertThat(allTime.getTotalCarsAvoided()).isEqualTo(4);
        assertThat(allTime.getActiveUsers()).isEqualTo(4);
        assertThat(platformIndicator(IndicatorPeriod.MONTHLY, "2026-11").getActiveUsers()).isEqualTo(4);

        UserIndicator driver = userIndicator("driver", IndicatorPeriod.MONTHLY, "2026-11");
        assertThat(driver.getRidesAsDriver()).isEqualTo(2);
        assertThat(driver.getSharedKm()).isEqualTo(34.0);
        assertThat(driver.getCarsAvoided()).isEqualTo(4);

        assertThat(userIndicator("p1", IndicatorPeriod.MONTHLY, "2026-11").getRidesAsPassenger()).isEqualTo(2);
        assertThat(userIndicator("p3", IndicatorPeriod.MONTHLY, "2026-11").getRidesAsPassenger()).isEqualTo(1);
    }

    @Test
    @DisplayName("Viagem em outro mês deve abrir novos documentos de período sem recontar o usuário no acumulado")
    void shouldOpenNewPeriodDocumentsWithoutRecountingAllTimeUsers() {
        registerCompletedRide.execute(ride("driver", List.of("p1"), 24.0));

        registerCompletedRide.execute(new CompletedRideCommand(
                "ride-2", "driver", List.of("p1"), 24.0, "GASOLINE", 12.0, LocalDateTime.of(2026, 12, 3, 8, 0)));

        assertThat(platformIndicator(IndicatorPeriod.MONTHLY, "2026-11").getTotalRides()).isEqualTo(1);
        assertThat(platformIndicator(IndicatorPeriod.MONTHLY, "2026-12").getTotalRides()).isEqualTo(1);
        assertThat(platformIndicator(IndicatorPeriod.MONTHLY, "2026-12").getActiveUsers()).isEqualTo(2);
        assertThat(platformIndicator(IndicatorPeriod.SEMESTERLY, "2026-S2").getTotalRides()).isEqualTo(2);
        assertThat(platformIndicator(IndicatorPeriod.SEMESTERLY, "2026-S2").getActiveUsers()).isEqualTo(2);
        assertThat(platformIndicator(IndicatorPeriod.ALL_TIME, "ALL_TIME").getTotalRides()).isEqualTo(2);
        assertThat(platformIndicator(IndicatorPeriod.ALL_TIME, "ALL_TIME").getActiveUsers()).isEqualTo(2);
    }

    @Test
    @DisplayName("Sem dados do veículo, a viagem conta mas o CO2 evitado fica zerado")
    void shouldCountRideWithZeroCo2WhenVehicleDataIsMissing() {
        registerCompletedRide.execute(new CompletedRideCommand(
                "ride-1", "driver", List.of("p1"), 24.0, null, null, COMPLETED_AT));

        PlatformIndicator allTime = platformIndicator(IndicatorPeriod.ALL_TIME, "ALL_TIME");
        assertThat(allTime.getTotalRides()).isEqualTo(1);
        assertThat(allTime.getTotalSharedKm()).isEqualTo(24.0);
        assertThat(allTime.getTotalCo2AvoidedKg()).isZero();
    }

    @Test
    @DisplayName("Passageiro repetido na lista deve ser contado uma vez só")
    void shouldCountDuplicatedPassengerOnce() {
        registerCompletedRide.execute(ride("driver", List.of("p1", "p1"), 24.0));

        assertThat(platformIndicator(IndicatorPeriod.ALL_TIME, "ALL_TIME").getTotalCarsAvoided()).isEqualTo(1);
        assertThat(userIndicator("p1", IndicatorPeriod.MONTHLY, "2026-11").getRidesAsPassenger()).isEqualTo(1);
    }

    @Test
    @DisplayName("Viagem sem passageiro efetivo não entra nos indicadores (RF49)")
    void shouldRejectRideWithoutEffectivePassengers() {
        assertThatThrownBy(() -> registerCompletedRide.execute(ride("driver", List.of(), 24.0)))
                .isInstanceOf(ApplicationException.class);

        assertThat(users).isEmpty();
        assertThat(platform).isEmpty();
    }

    @Test
    @DisplayName("Deve recusar distância negativa e viagem sem motorista ou sem data de conclusão")
    void shouldRejectInvalidCommand() {
        assertThatThrownBy(() -> registerCompletedRide.execute(ride("driver", List.of("p1"), -1.0)))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> registerCompletedRide.execute(ride(" ", List.of("p1"), 24.0)))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> registerCompletedRide.execute(new CompletedRideCommand(
                "ride-1", "driver", List.of("p1"), 24.0, "GASOLINE", 12.0, null)))
                .isInstanceOf(ApplicationException.class);

        assertThat(users).isEmpty();
        assertThat(platform).isEmpty();
    }
}
