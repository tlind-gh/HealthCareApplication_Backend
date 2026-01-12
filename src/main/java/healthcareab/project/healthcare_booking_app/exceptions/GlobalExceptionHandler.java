package healthcareab.project.healthcare_booking_app.exceptions;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.DispatcherServlet;

@ControllerAdvice
public class GlobalExceptionHandler {
    private final HttpServletRequest httpServletRequest;
    private final DispatcherServlet dispatcherServlet;
    private final DispatcherServletAutoConfiguration dispatcherServletAutoConfiguration;
    
    
    public GlobalExceptionHandler(HttpServletRequest httpServletRequest, DispatcherServlet dispatcherServlet, DispatcherServletAutoConfiguration dispatcherServletAutoConfiguration) {
        this.httpServletRequest = httpServletRequest;
        this.dispatcherServlet = dispatcherServlet;
        this.dispatcherServletAutoConfiguration = dispatcherServletAutoConfiguration;
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> illegalArgumentExceptionHandler(IllegalArgumentException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<String> unauthorizedExceptionHandler(UnauthorizedException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }
}