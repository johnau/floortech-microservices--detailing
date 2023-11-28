package tech.jmcs.floortech.detailing.presentation.web.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import tech.jmcs.floortech.detailing.presentation.web.error.ErrorDto;

public interface GlobalRoutingHandler {
    /**
     * Helper method for generic request processing and error handling
      * @param action
     * @param httpStatus
     * @return
     * @param <T>
     */
    static <T> Mono<ServerResponse> doRequest(Mono<T> action, HttpStatus httpStatus) {
        return action
                .flatMap(result -> ServerResponse
                        .status(httpStatus)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(result))
                )
                .onErrorResume(error -> ServerResponse
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(new ErrorDto(error.getMessage())))
                );
    }

}
