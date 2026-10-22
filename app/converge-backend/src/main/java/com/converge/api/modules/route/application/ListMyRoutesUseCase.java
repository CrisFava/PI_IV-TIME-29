package com.converge.api.modules.route.application;

import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyRoutesUseCase implements UseCase<String, List<Route>> {

    private final RouteRepository routeRepository;

    @Override
    public List<Route> execute(String userId) {
        return routeRepository.findByUserIdAndStatusNot(userId, RouteStatus.INACTIVE);
    }

}
