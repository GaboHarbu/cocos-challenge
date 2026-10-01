package com.cocos.challenge.application.mapper;

import com.cocos.challenge.application.dto.portfolio.PortfolioResponse;
import com.cocos.challenge.application.dto.portfolio.PositionResponse;
import com.cocos.challenge.domain.model.Portfolio;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PortfolioMapper {

    public PortfolioResponse toResponse(Portfolio portfolio) {
        if (portfolio == null) {
            return null;
        }

        List<PositionResponse> positionResponses = portfolio.positions().stream()
                .map(PositionResponse::from)
                .toList();

        return new PortfolioResponse(
                portfolio.userId(),
                portfolio.accountNumber(),
                portfolio.totalAccountValue(),
                portfolio.availableCash(),
                portfolio.totalCash(),
                positionResponses
        );
    }
}
