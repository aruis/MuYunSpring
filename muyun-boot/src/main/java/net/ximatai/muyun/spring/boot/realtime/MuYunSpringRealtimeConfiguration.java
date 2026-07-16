package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.boot.platform.PlatformRecordActionAvailabilityService;
import net.ximatai.muyun.spring.boot.web.MuYunSpringCorsProperties;
import net.ximatai.muyun.spring.iam.user.UserSecurityEventPublisher;
import net.ximatai.muyun.spring.iam.user.UserSessionLifecycleEventPublisher;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class MuYunSpringRealtimeConfiguration implements WebSocketMessageBrokerConfigurer {
    private final UserSessionService userSessionService;
    private final RealtimeConnectionRegistry connectionRegistry;
    private final MuYunSpringCorsProperties corsProperties;

    public MuYunSpringRealtimeConfiguration(UserSessionService userSessionService,
                                            RealtimeConnectionRegistry connectionRegistry,
                                            ObjectProvider<MuYunSpringCorsProperties> corsProperties) {
        this.userSessionService = userSessionService;
        this.connectionRegistry = connectionRegistry;
        this.corsProperties = corsProperties.getIfAvailable();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var endpoint = registry.addEndpoint("/ws/platform");
        List<String> allowedOrigins = corsProperties == null ? List.of() : corsProperties.getAllowedOrigins();
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            endpoint.setAllowedOrigins(allowedOrigins.toArray(String[]::new));
        } else {
            endpoint.setAllowedOriginPatterns("*");
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new RealtimeAuthenticationChannelInterceptor(userSessionService,
                connectionRegistry));
    }

    @Bean
    @ConditionalOnMissingBean(RealtimeConnectionRegistry.class)
    public static RealtimeConnectionRegistry realtimeConnectionRegistry() {
        return new RealtimeConnectionRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(RealtimeMessagePublisher.class)
    public RealtimeMessagePublisher realtimeMessagePublisher(SimpMessagingTemplate messagingTemplate) {
        return new StompRealtimeMessagePublisher(messagingTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(DataChangeRealtimePublisher.class)
    public DataChangeRealtimePublisher dataChangeRealtimePublisher(RealtimeMessagePublisher messagePublisher) {
        return new StompDataChangeRealtimePublisher(messagePublisher);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityRealtimeNotifier.class)
    public SecurityRealtimeNotifier securityRealtimeNotifier(RealtimeMessagePublisher messagePublisher) {
        return new StompSecurityRealtimeNotifier(messagePublisher);
    }

    @Bean
    @ConditionalOnMissingBean(BusinessRealtimeNotifier.class)
    public BusinessRealtimeNotifier businessRealtimeNotifier(RealtimeMessagePublisher messagePublisher) {
        return new StompBusinessRealtimeNotifier(messagePublisher);
    }

    @Bean
    @ConditionalOnMissingBean(BusinessRealtimeFanOutPublisher.class)
    public BusinessRealtimeFanOutPublisher businessRealtimeFanOutPublisher(
            BusinessRealtimeNotifier businessRealtimeNotifier) {
        return new OnlineUserBusinessRealtimeFanOutPublisher(
                connectionRegistry, userSessionService, businessRealtimeNotifier);
    }

    @Bean
    @ConditionalOnMissingBean(BusinessRealtimeRecipientPolicyFactory.class)
    @ConditionalOnBean(PlatformRecordActionAvailabilityService.class)
    public BusinessRealtimeRecipientPolicyFactory businessRealtimeRecipientPolicyFactory(
            PlatformRecordActionAvailabilityService actionAvailabilityService) {
        return new BusinessRealtimeRecipientPolicyFactory(actionAvailabilityService);
    }

    @Bean
    @ConditionalOnMissingBean(UserSecurityEventPublisher.class)
    public UserSecurityEventPublisher userSecurityEventPublisher(SecurityRealtimeNotifier securityRealtimeNotifier) {
        return new UserSecurityRealtimeEventPublisher(securityRealtimeNotifier);
    }

    @Bean
    @ConditionalOnMissingBean(UserSessionLifecycleEventPublisher.class)
    @ConditionalOnBean(PlatformRecordActionAvailabilityService.class)
    public UserSessionLifecycleEventPublisher userSessionLifecycleEventPublisher(
            BusinessRealtimeFanOutPublisher businessRealtimeFanOutPublisher,
            BusinessRealtimeRecipientPolicyFactory recipientPolicyFactory) {
        return new UserSessionManagementRealtimeEventPublisher(
                businessRealtimeFanOutPublisher, recipientPolicyFactory);
    }
}
