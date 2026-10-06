package br.com.rafael.aigateway.api;

import br.com.rafael.aigateway.core.PromptBloqueadoException;
import br.com.rafael.aigateway.provider.PerfilNaoSuportadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PerfilNaoSuportadoException.class)
    public ProblemDetail perfilNaoSuportado(PerfilNaoSuportadoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(PromptBloqueadoException.class)
    public ProblemDetail promptBloqueado(PromptBloqueadoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
