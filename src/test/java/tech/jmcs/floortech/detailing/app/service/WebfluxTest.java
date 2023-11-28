package tech.jmcs.floortech.detailing.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

@WebFluxTest
@ExtendWith(SpringExtension.class)
public class WebfluxTest {
//    @Test
//    public void webflux() {
//        var mono = Mono.just("test")
//                .doOnError(error -> System.out.println("There was an error"))
//                .map(string -> string + "!")
//                .map(string -> Integer.parseInt(string))
//                .doOnError(error -> System.out.println("There was an error parsing Integer"))
//                .block();
//    }
}
