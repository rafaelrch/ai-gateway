package br.com.rafael.aigateway.api;

import br.com.rafael.aigateway.core.PromptBloqueadoException;
import br.com.rafael.aigateway.provider.PerfilNaoSuportadoException;
import br.com.rafael.aigateway.provider.ProvedorIndisponivelException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduz exceção em resposta HTTP, num lugar só.
 *
 * @RestControllerAdvice: o Spring consulta esta classe sempre que um
 * controller deixa uma exceção escapar.
 *
 * Estender ResponseEntityExceptionHandler faz os erros padrão do Spring
 * (JSON malformado, perfil inexistente no enum, @NotBlank violado) também
 * saírem no formato ProblemDetail. Assim todo erro da API tem o mesmo
 * formato: type, title, status, detail, instance (RFC 9457).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Perfil sem provedor registrado -> 400 Bad Request.
    @ExceptionHandler(PerfilNaoSuportadoException.class)
    public ProblemDetail perfilNaoSuportado(PerfilNaoSuportadoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // Prompt barrado pela corrente (tamanho ou termo) -> 400, com o motivo no "detail".
    @ExceptionHandler(PromptBloqueadoException.class)
    public ProblemDetail promptBloqueado(PromptBloqueadoException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        // Título próprio para o cliente distinguir de um 400 de validação comum.
        problema.setTitle("Prompt bloqueado");
        return problema;
    }

    // Provedor falhou em todas as tentativas do retry -> 503 Service Unavailable.
    @ExceptionHandler(ProvedorIndisponivelException.class)
    public ProblemDetail provedorIndisponivel(ProvedorIndisponivelException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        problema.setTitle("Provedor indisponível");
        return problema;
    }

    // @Valid falhou (prompt vazio, perfil ausente). O padrão do Spring diz só
    // "Invalid request content."; aqui o detail lista campo por campo.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        // Junta "campo: mensagem" de cada erro, separados por "; ".
        String detalhe = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .sorted()
                .reduce((a, b) -> a + "; " + b)
                .orElse("requisição inválida");
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
        problema.setTitle("Requisição inválida");
        return ResponseEntity.badRequest().body(problema);
    }
}
