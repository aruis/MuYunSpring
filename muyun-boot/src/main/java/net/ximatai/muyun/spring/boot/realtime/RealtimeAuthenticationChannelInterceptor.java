package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;
import java.util.Optional;

public class RealtimeAuthenticationChannelInterceptor implements ChannelInterceptor {
    private final UserSessionService userSessionService;

    public RealtimeAuthenticationChannelInterceptor(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(command)) {
            String token = bearerToken(accessor);
            CurrentUser currentUser = authenticate(token)
                    .orElseThrow(() -> new IllegalArgumentException("realtime authentication required"));
            if (currentUser.passwordChangeRequired()) {
                throw new IllegalArgumentException("password change required");
            }
            accessor.setUser(new CurrentUserPrincipal(currentUser, token));
            return message;
        }
        if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
            CurrentUserPrincipal principal = currentUserPrincipal(accessor.getUser())
                    .orElseThrow(() -> new IllegalArgumentException("realtime authentication required"));
            CurrentUser currentUser = authenticate(principal.token())
                    .orElseThrow(() -> new IllegalArgumentException("realtime authentication required"));
            if (currentUser.passwordChangeRequired()) {
                throw new IllegalArgumentException("password change required");
            }
            accessor.setUser(new CurrentUserPrincipal(currentUser, principal.token()));
        }
        return message;
    }

    private Optional<CurrentUser> authenticate(String token) {
        return userSessionService.currentUser(token);
    }

    private String bearerToken(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || header.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (!header.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }
        return header.substring(prefix.length()).trim();
    }

    public static CurrentUser currentUser(Principal principal) {
        return currentUserPrincipal(principal)
                .map(CurrentUserPrincipal::currentUser)
                .orElse(null);
    }

    private static Optional<CurrentUserPrincipal> currentUserPrincipal(Principal principal) {
        if (principal instanceof CurrentUserPrincipal currentUserPrincipal) {
            return Optional.of(currentUserPrincipal);
        }
        return Optional.empty();
    }
}
