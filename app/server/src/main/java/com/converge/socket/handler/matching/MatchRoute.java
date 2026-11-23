// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.socket.handler.matching;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

/**
 * Um trajeto no formato que o servidor de sockets entende (sem classes do Spring nem GeoJSON).
 * Os campos de oferta ficam null num trajeto de procura.
 */
public class MatchRoute implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String routeId;
    private final String userId;
    private final double originLat;
    private final double originLng;
    private final double destinationLat;
    private final double destinationLng;
    private final String departureTime;
    private final Set<DayOfWeek> weekDays;

    // Só para OFFER
    private final Double maxDetourKm;
    private final Integer totalSeats;
    private final Integer availableSeats;
    private final Double routeDistanceKm;
    // Preço da energia dividido pelo consumo médio do veículo (R$/km). Null se o veículo não informar.
    private final BigDecimal energyCostPerKm;
    // Pedágio + estacionamento da viagem.
    private final BigDecimal fixedCosts;
    // Caminho do motorista pelas ruas (polyline do Google). Null: usa a linha reta origem → destino.
    private final String encodedPolyline;

    /**
     * Construtor para rota sem polyline codificada (utiliza trajeto em linha reta entre origem e destino).
     *
     * @param routeId identificador único do trajeto
     * @param userId identificador do usuário proprietário do trajeto
     * @param originLat latitude de origem
     * @param originLng longitude de origem
     * @param destinationLat latitude de destino
     * @param destinationLng longitude de destino
     * @param departureTime horário de saída no formato HH:mm
     * @param weekDays conjunto de dias da semana em que a viagem ocorre
     * @param maxDetourKm desvio máximo tolerado em quilômetros (apenas para oferta)
     * @param totalSeats total de vagas no veículo (apenas para oferta)
     * @param availableSeats vagas disponíveis para carona (apenas para oferta)
     * @param routeDistanceKm distância total do trajeto em quilômetros (apenas para oferta)
     * @param energyCostPerKm custo de combustível/energia por quilômetro (apenas para oferta)
     * @param fixedCosts custos fixos somados (pedágio e estacionamento)
     */
    public MatchRoute(
            String routeId,
            String userId,
            double originLat,
            double originLng,
            double destinationLat,
            double destinationLng,
            String departureTime,
            Set<DayOfWeek> weekDays,
            Double maxDetourKm,
            Integer totalSeats,
            Integer availableSeats,
            Double routeDistanceKm,
            BigDecimal energyCostPerKm,
            BigDecimal fixedCosts
    ) {
        this(routeId, userId, originLat, originLng, destinationLat, destinationLng, departureTime, weekDays,
                maxDetourKm, totalSeats, availableSeats, routeDistanceKm, energyCostPerKm, fixedCosts, null);
    }

    /**
     * Construtor completo para rota com polyline codificada da rota pelas ruas.
     *
     * @param routeId identificador único do trajeto
     * @param userId identificador do usuário proprietário do trajeto
     * @param originLat latitude de origem
     * @param originLng longitude de origem
     * @param destinationLat latitude de destino
     * @param destinationLng longitude de destino
     * @param departureTime horário de saída no formato HH:mm
     * @param weekDays conjunto de dias da semana em que a viagem ocorre
     * @param maxDetourKm desvio máximo tolerado em quilômetros
     * @param totalSeats total de vagas no veículo
     * @param availableSeats vagas disponíveis para carona
     * @param routeDistanceKm distância total do trajeto em quilômetros
     * @param energyCostPerKm custo de combustível/energia por quilômetro
     * @param fixedCosts custos fixos somados
     * @param encodedPolyline linha poligonal codificada representando o trajeto geográfico
     */
    public MatchRoute(
            String routeId,
            String userId,
            double originLat,
            double originLng,
            double destinationLat,
            double destinationLng,
            String departureTime,
            Set<DayOfWeek> weekDays,
            Double maxDetourKm,
            Integer totalSeats,
            Integer availableSeats,
            Double routeDistanceKm,
            BigDecimal energyCostPerKm,
            BigDecimal fixedCosts,
            String encodedPolyline
    ) {
        this.routeId = routeId;
        this.userId = userId;
        this.originLat = originLat;
        this.originLng = originLng;
        this.destinationLat = destinationLat;
        this.destinationLng = destinationLng;
        this.departureTime = departureTime;
        // EnumSet é serializável e evita depender da implementação de Set de quem chamou
        this.weekDays = weekDays == null || weekDays.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class)
                : EnumSet.copyOf(weekDays);
        this.maxDetourKm = maxDetourKm;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.routeDistanceKm = routeDistanceKm;
        this.energyCostPerKm = energyCostPerKm;
        this.fixedCosts = fixedCosts;
        this.encodedPolyline = encodedPolyline;
    }

    /**
     * Cria uma rota simplificada representando a procura de um passageiro (SEARCH).
     *
     * @param routeId identificador do trajeto
     * @param userId identificador do passageiro
     * @param originLat latitude de embarque
     * @param originLng longitude de embarque
     * @param destinationLat latitude de desembarque
     * @param destinationLng longitude de desembarque
     * @param departureTime horário desejado de saída (HH:mm)
     * @param weekDays dias da semana desejados
     * @return instância de {@link MatchRoute} configurada para procura
     */
    public static MatchRoute search(
            String routeId,
            String userId,
            double originLat,
            double originLng,
            double destinationLat,
            double destinationLng,
            String departureTime,
            Set<DayOfWeek> weekDays
    ) {
        return new MatchRoute(routeId, userId, originLat, originLng, destinationLat, destinationLng,
                departureTime, weekDays, null, null, null, null, null, null);
    }

    // Getters e setters
    public String getRouteId() {
        return routeId;
    }

    public String getUserId() {
        return userId;
    }

    public double getOriginLat() {
        return originLat;
    }

    public double getOriginLng() {
        return originLng;
    }

    public double getDestinationLat() {
        return destinationLat;
    }

    public double getDestinationLng() {
        return destinationLng;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public Set<DayOfWeek> getWeekDays() {
        return weekDays;
    }

    public Double getMaxDetourKm() {
        return maxDetourKm;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public Double getRouteDistanceKm() {
        return routeDistanceKm;
    }

    public BigDecimal getEnergyCostPerKm() {
        return energyCostPerKm;
    }

    public BigDecimal getFixedCosts() {
        return fixedCosts;
    }

    public String getEncodedPolyline() {
        return encodedPolyline;
    }

    @Override
    public String toString() {
        return "MatchRoute{routeId='" + routeId + "', departureTime='" + departureTime + "'}";
    }
}
