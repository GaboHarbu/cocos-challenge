package com.cocos.challenge.infrastructure.web.controller;

import com.cocos.challenge.application.dto.instrument.InstrumentDto;
import com.cocos.challenge.application.service.InstrumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentService instrumentService;

    /**
     * Search instruments by ticker or name with pagination:
     * GET /api/v1/instruments?query=GGAL&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<InstrumentDto>> search(
            @RequestParam(value = "query", required = false, defaultValue = "") String query,
            @PageableDefault(page = 0, size = 20, sort = "ticker", direction = Sort.Direction.ASC) Pageable pageable) {
        var results = instrumentService.search(query, pageable);
        return ResponseEntity.ok(results);
    }
}
