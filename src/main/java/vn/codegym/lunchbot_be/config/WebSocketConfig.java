package vn.codegym.lunchbot_be.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import vn.codegym.lunchbot_be.util.JwtUtil;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    private final Map<String, AtomicInteger> connectionAttempts = new ConcurrentHashMap<>();
    private static final int MAX_CONNECTION_ATTEMPTS = 3;

    @Bean
    public ThreadPoolTaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{20000, 20000})
                .setTaskScheduler(heartbeatScheduler());
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js")
                .setDisconnectDelay(3000)
                .setHeartbeatTime(20000)
                .setSessionCookieNeeded(false)
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(100);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(128 * 1024)
                .setSendBufferSizeLimit(512 * 1024)
                .setSendTimeLimit(10000)
                .setTimeToFirstMessage(10000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    StompCommand command = accessor.getCommand();
                    String sessionId = accessor.getSessionId();

                    // CONNECT command
                    if (StompCommand.CONNECT.equals(command)) {
                        String authHeader = accessor.getFirstNativeHeader("Authorization");

                        // ✅ CHO PHÉP ANONYMOUS - Kiểm tra token NẾU CÓ
                        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                            // ✅ KHÔNG CÓ TOKEN - Cho phép anonymous connection
                            log.info("✅ WebSocket anonymous connection: session={}", sessionId);
                            return message; // ✅ CHO PHÉP kết nối
                        }

                        // CÓ TOKEN - Validate và authenticate
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

                                log.info("✅ WebSocket authenticated: user={}, session={}", email, sessionId);
                            } else {
                                log.warn("⚠️ Invalid JWT token, allowing anonymous: session={}", sessionId);
                                // ✅ Token không hợp lệ nhưng VẪN CHO PHÉP connect
                                return message;
                            }
                        } catch (Exception e) {
                            log.error("❌ JWT validation error, allowing anonymous: session={}, error={}",
                                    sessionId, e.getMessage());
                            // ✅ Lỗi validate nhưng VẪN CHO PHÉP connect
                            return message;
                        }
                    }

                    // DISCONNECT command
                    else if (StompCommand.DISCONNECT.equals(command)) {
                        String user = accessor.getUser() != null ? accessor.getUser().getName() : "anonymous";
                        log.info("🔌 WebSocket DISCONNECT: user={}, session={}", user, sessionId);
                        connectionAttempts.remove(sessionId);
                    }

                    // SUBSCRIBE command
                    else if (StompCommand.SUBSCRIBE.equals(command)) {
                        String destination = accessor.getDestination();
                        String user = accessor.getUser() != null ? accessor.getUser().getName() : "anonymous";
                        log.debug("📬 WebSocket SUBSCRIBE: user={}, destination={}, session={}",
                                user, destination, sessionId);
                    }

                    // UNSUBSCRIBE command
                    else if (StompCommand.UNSUBSCRIBE.equals(command)) {
                        String user = accessor.getUser() != null ? accessor.getUser().getName() : "anonymous";
                        log.debug("🔌 WebSocket UNSUBSCRIBE: user={}, session={}", user, sessionId);
                    }

                    // SEND command
                    else if (StompCommand.SEND.equals(command)) {
                        String destination = accessor.getDestination();
                        String user = accessor.getUser() != null ? accessor.getUser().getName() : "anonymous";
                        log.debug("📤 WebSocket SEND: user={}, destination={}, session={}",
                                user, destination, sessionId);
                    }
                }

                return message;
            }

            @Override
            public void afterSendCompletion(Message<?> message, MessageChannel channel,
                                            boolean sent, Exception ex) {
                if (ex != null) {
                    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                    if (accessor != null) {
                        String sessionId = accessor.getSessionId();
                        log.error("❌ Error sending WebSocket message: session={}, error={}",
                                sessionId, ex.getMessage());
                        connectionAttempts.remove(sessionId);
                    }
                }
            }
        });

        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(8)
                .keepAliveSeconds(60);
    }
}