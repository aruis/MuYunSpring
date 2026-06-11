package net.ximatai.muyun.spring.platform.config;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LowCodeModuleTemplateService {
    private final LowCodeModulePackageExchangeService exchangeService;

    public LowCodeModuleTemplateService(LowCodeModulePackageExchangeService exchangeService) {
        this.exchangeService = exchangeService;
    }

    public LowCodeModuleTemplate createTemplateFromVersion(String templateAlias,
                                                           String title,
                                                           String versionId) {
        LowCodeModulePackage source = exchangeService.parsePackage(exchangeService.exportVersionPackage(versionId));
        requireTemplateSource(source);
        LowCodeModulePackage templatePackage = new LowCodeModulePackage(
                source.protocolVersion(),
                LowCodePackageMode.TEMPLATE,
                source.applicationAlias(),
                source.moduleAlias(),
                source.bundles(),
                source.dependencyManifest(),
                source.publishManifest()
        );
        return new LowCodeModuleTemplate(templateAlias, title, templatePackage);
    }

    public LowCodeModulePackage instantiate(LowCodeModuleTemplate template,
                                            LowCodeModuleTemplateInstantiationRequest request) {
        if (template == null) {
            throw new IllegalArgumentException("template must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("template instantiation request must not be null");
        }
        LowCodeModulePackage base = template.basePackage();
        return new LowCodeModulePackage(
                base.protocolVersion(),
                LowCodePackageMode.MODULE_FULL,
                request.applicationAlias(),
                request.moduleAlias(),
                rewriteBundles(base, request),
                base.dependencyManifest(),
                LowCodePackagePublishManifest.draft(base.protocolVersion())
        );
    }

    private void requireTemplateSource(LowCodeModulePackage source) {
        if (source.mode() != LowCodePackageMode.MODULE_FULL || !source.includes(LowCodePackageBundleType.METADATA)) {
            throw new IllegalArgumentException("template source package must be MODULE_FULL with METADATA bundle");
        }
    }

    private List<LowCodeConfigBundle> rewriteBundles(LowCodeModulePackage base,
                                                     LowCodeModuleTemplateInstantiationRequest request) {
        return base.bundles().stream()
                .map(bundle -> new LowCodeConfigBundle(bundle.type(), bundle.included(),
                        rewriteContent(bundle.type(), bundle.content(), base.moduleAlias(), request)))
                .toList();
    }

    private Map<String, Object> rewriteContent(LowCodePackageBundleType bundleType,
                                               Map<String, Object> content,
                                               String sourceModuleAlias,
                                               LowCodeModuleTemplateInstantiationRequest request) {
        if (content == null || content.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> rewritten = new LinkedHashMap<>(content);
        replaceIfEquals(rewritten, "module", sourceModuleAlias, request.moduleAlias());
        replaceIfEquals(rewritten, "moduleAlias", sourceModuleAlias, request.moduleAlias());
        if (bundleType == LowCodePackageBundleType.METADATA) {
            if (request.title() != null) {
                rewritten.put("title", request.title());
            }
            for (Map.Entry<String, Object> parameter : request.parameters().entrySet()) {
                if (parameter.getKey() != null && !parameter.getKey().isBlank()) {
                    rewritten.put(parameter.getKey(), parameter.getValue());
                }
            }
        }
        return rewritten;
    }

    private void replaceIfEquals(Map<String, Object> content,
                                 String key,
                                 String expected,
                                 String replacement) {
        if (expected.equals(content.get(key))) {
            content.put(key, replacement);
        }
    }

}
