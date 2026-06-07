package br.ifg.urutai.sdapientrega;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação sd-api-entrega.
 *
 * O registro no Eureka Server é feito automaticamente pela auto-configuração do
 * spring-cloud-starter-netflix-eureka-client quando a dependência está presente
 * no classpath.
 */
@SpringBootApplication
public class SdApiEntregaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SdApiEntregaApplication.class, args);
    }

}
