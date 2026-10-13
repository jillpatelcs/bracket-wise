package ca.bracketwise.config;

import ca.bracketwise.domain.calc.PlanningDefaults;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestSizeFilter extends OncePerRequestFilter {
    private final int maxBytes;

    public RequestSizeFilter(PlanningDefaults defaults) {
        this.maxBytes = defaults.maxRequestBytes();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long length = request.getContentLengthLong();
        if (length > maxBytes) {
            writeTooLarge(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeTooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(413);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String body = """
                {"type":"about:blank","title":"Payload Too Large","status":413,"detail":"Request body exceeds 16 KB.","code":"PAYLOAD_TOO_LARGE","errors":[{"field":null,"code":"PAYLOAD_TOO_LARGE","message":"Request body exceeds 16 KB."}]}
                """;
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }
}
