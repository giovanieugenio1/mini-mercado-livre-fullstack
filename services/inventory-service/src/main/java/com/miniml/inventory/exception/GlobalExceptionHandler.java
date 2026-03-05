package com.miniml.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReservationNotFoundException.class)
    public ProblemDetail handleNotFound(ReservationNotFoundException ex) {
        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Reserva não encontrada");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ProductStockNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductStockNotFoundException ex) {
        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Produto não encontrado no inventário");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleConflict(IllegalStateException ex) {
        var pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Operação inválida");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Parâmetro inválido");
        pd.setDetail("Valor inválido para o parâmetro '" + ex.getName() + "': " + ex.getValue());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Erro interno");
        pd.setDetail("Ocorreu um erro inesperado.");
        return pd;
    }
}
