package com.cocos.challenge.domain.exception;

public class InstrumentNotFoundException extends DomainException {
    public InstrumentNotFoundException(Long instrumentId) {
        super("Instrument not found with id: " + instrumentId);
    }

    public InstrumentNotFoundException(String ticker) {
        super("Instrument not found with ticker: " + ticker);
    }
}
