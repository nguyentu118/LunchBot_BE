package vn.codegym.lunchbot_be.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import vn.codegym.lunchbot_be.util.JwtUtil;

import java.util.Collection;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    StompCommand command = accessor.getCommand();

                    // ✅ Log CONNECT command
                    if (StompCommand.CONNECT.equals(command)) {
                        String authHeader = accessor.getFirstNativeHeader("Authorization");

                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            String token = authHeader.substring(7);

                            try {
                                if (jwtUtil.validateToken(token)) {
                                    String email = jwtUtil.extractEmail(token);
                                    Long userId = jwtUtil.extractUserId(token);
                                    Collection<? extends GrantedAuthority> authorities =
                                            jwtUtil.getAuthorities(token);

                                    UsernamePasswordAuthenticationToken authentication =
                                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                                    authentication.setDetails(userId);
                                    accessor.setUser(authentication);

                                } else {
                                    return null;
                                }
                            } catch (Exception e) {
                                return null;
                            }
                        } else {
                            return null;
                        }
                    }

                    // ✅ Log SUBSCRIBE command
                    else if (StompCommand.SUBSCRIBE.equals(command)) {
                        String destination = accessor.getDestination();
                        String user = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";
                        String subscriptionId = accessor.getSubscriptionId();

                    }

                    // ✅ Log UNSUBSCRIBE command
                    else if (StompCommand.UNSUBSCRIBE.equals(command)) {
                        String user = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";
                        String subscriptionId = accessor.getSubscriptionId();
                    }

                    // ✅ Log DISCONNECT command
                    else if (StompCommand.DISCONNECT.equals(command)) {
                        String user = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";
                    }

                    // ✅ Log MESSAGE/SEND commands
                    else if (StompCommand.SEND.equals(command)) {
                        String destination = accessor.getDestination();
                        String user = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";
                    }
                }

                return message;
            }
        });
    }
}