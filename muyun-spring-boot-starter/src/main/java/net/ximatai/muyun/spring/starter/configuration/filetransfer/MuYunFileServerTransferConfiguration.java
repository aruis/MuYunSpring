package net.ximatai.muyun.spring.starter.configuration.filetransfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.platform.attachment.FileTransferAccessService;
import net.ximatai.muyun.spring.platform.attachment.FileTransferClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Registers the official FileServer adapter only when an application explicitly enables it. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MuYunFileServerTransferProperties.class)
@ConditionalOnProperty(prefix = "muyun.file-transfer.muyun-fileserver", name = "enabled", havingValue = "true")
public class MuYunFileServerTransferConfiguration {
    @Bean
    @ConditionalOnMissingBean(FileTransferAccessService.class)
    MuYunFileServerTransferAccessService muYunFileServerTransferAccessService(
            MuYunFileServerTransferProperties properties,
            ObjectMapper objectMapper) {
        return new MuYunFileServerTransferAccessService(properties, objectMapper);
    }

    @Bean
    @ConditionalOnBean(MuYunFileServerTransferAccessService.class)
    @ConditionalOnMissingBean(FileTransferClient.class)
    FileTransferClient fileTransferClient(MuYunFileServerTransferAccessService accessService,
                                          ObjectMapper objectMapper) {
        return new MuYunFileServerTransferClient(accessService, RestClient.builder().build(), objectMapper);
    }
}
