package com.cocos.challenge.infrastructure.web.controller;

import com.cocos.challenge.application.dto.portfolio.PortfolioResponse;
import com.cocos.challenge.application.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * Get user portfolio via query parameter: GET /api/v1/portfolio?userId=1
     */
    @GetMapping
    public ResponseEntity<PortfolioResponse> getPortfolioByParam(@RequestParam("userId") Long userId) {
        var response = portfolioService.getPortfolio(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user portfolio via path variable: GET /api/v1/portfolio/1
     */
    @GetMapping("/{userId}")
    public ResponseEntity<PortfolioResponse> getPortfolioByPath(@PathVariable("userId") Long userId) {
        var response = portfolioService.getPortfolio(userId);
        return ResponseEntity.ok(response);
    }
}
