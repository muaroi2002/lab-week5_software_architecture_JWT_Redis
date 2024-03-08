package Service;

import org.springframework.security.core.token.Token;

public interface TokenService {
    Token createToken(Token token);

    Token findByToken(String token);
}
