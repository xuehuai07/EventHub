package com.eventhub.assistant;

import com.eventhub.assistant.AssistantDtos.ErrorEvent;
import com.eventhub.common.error.ErrorCode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class AssistantSseEvents {

    private static final long EMITTER_TIMEOUT_MILLIS = 65_000;

    public SseEmitter create() {
        return new SseEmitter(EMITTER_TIMEOUT_MILLIS);
    }

    public void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public void error(SseEmitter emitter, AtomicBoolean terminal, ErrorCode errorCode, String message) {
        if (!terminal.compareAndSet(false, true)) {
            return;
        }
        try {
            send(
                    emitter,
                    "error",
                    new ErrorEvent(
                            errorCode.code(),
                            message == null ? errorCode.message() : message,
                            errorCode == ErrorCode.AI_UPSTREAM_UNAVAILABLE));
            emitter.complete();
        } catch (RuntimeException exception) {
            emitter.completeWithError(exception);
        }
    }
}
