package br.com.rafael.aigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de partida da aplicação.
 *
 * @SpringBootApplication junta três coisas:
 * - @Configuration: esta classe pode declarar beans;
 * - @EnableAutoConfiguration: o Boot configura sozinho o Tomcat, o Jackson,
 *   a validação etc., olhando o que está no classpath;
 * - @ComponentScan: procura @Component, @Service, @Repository e
 *   @RestController neste pacote e nos de baixo, e cria um bean (singleton)
 *   de cada. É assim que os provedores e os handlers são encontrados sem
 *   ninguém registrar na mão.
 */
@SpringBootApplication
public class AiGatewayApplication {

	// main comum de Java: sobe o container do Spring e o servidor web na 8080.
	public static void main(String[] args) {
		SpringApplication.run(AiGatewayApplication.class, args);
	}

}
