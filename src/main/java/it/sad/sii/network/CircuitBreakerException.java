package it.sad.sii.network;

/**
 * Created by oskar on 7/12/16.
 */
public class CircuitBreakerException extends Exception {
    public CircuitBreakerException(String message) {
        super(message);
    }
}
