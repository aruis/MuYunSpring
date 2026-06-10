package net.ximatai.muyun.spring.platform.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformPagePreferenceService extends AbstractAbilityService<PlatformPagePreference> {
    public static final String MODULE_ALIAS = "platform.page_preference";
    public static final String DEFAULT_PAGE_KEY = "default";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public PlatformPagePreferenceService(BaseDao<PlatformPagePreference, String> preferenceDao) {
        super(MODULE_ALIAS, PlatformPagePreference.class, preferenceDao);
    }

    public PlatformPagePreference currentUserPreference(String moduleAlias,
                                                        PlatformUiClientType clientType,
                                                        String pageKey) {
        return findOne(scopeCriteria(currentUserId(), moduleAlias, clientType, pageKey));
    }

    @Transactional
    public PlatformPagePreference saveCurrentUserPreference(String moduleAlias,
                                                            PlatformUiClientType clientType,
                                                            String pageKey,
                                                            String preferenceJson) {
        String userId = currentUserId();
        Criteria scope = scopeCriteria(userId, moduleAlias, clientType, pageKey);
        PlatformPagePreference preference = findOne(scope);
        if (preference == null) {
            preference = new PlatformPagePreference();
            preference.setUserId(userId);
            preference.setModuleAlias(normalizeModuleAlias(moduleAlias));
            preference.setClientType(normalizeClientType(clientType).name());
            preference.setPageKey(normalizePageKey(pageKey));
            preference.setPreferenceJson(requirePreferenceJson(preferenceJson));
            insert(preference);
            return preference;
        }
        preference.setPreferenceJson(requirePreferenceJson(preferenceJson));
        update(preference);
        return select(preference.getId());
    }

    private Criteria scopeCriteria(String userId,
                                   String moduleAlias,
                                   PlatformUiClientType clientType,
                                   String pageKey) {
        return Criteria.of()
                .eq("userId", requireText(userId, "userId"))
                .eq("moduleAlias", normalizeModuleAlias(moduleAlias))
                .eq("clientType", normalizeClientType(clientType).name())
                .eq("pageKey", normalizePageKey(pageKey));
    }

    private String currentUserId() {
        CurrentUser user = CurrentUserContext.currentUser()
                .orElseThrow(() -> new PlatformException("page preference requires current user"));
        if (user.system()) {
            throw new PlatformException("page preference requires tenant user");
        }
        return user.userId();
    }

    private String normalizeModuleAlias(String moduleAlias) {
        return PlatformNameRules.requireModuleAlias(moduleAlias);
    }

    private PlatformUiClientType normalizeClientType(PlatformUiClientType clientType) {
        return clientType == null ? PlatformUiClientType.WEB : clientType;
    }

    private String normalizePageKey(String pageKey) {
        return pageKey == null || pageKey.isBlank() ? DEFAULT_PAGE_KEY : pageKey.trim();
    }

    private String requirePreferenceJson(String preferenceJson) {
        String normalized = requireText(preferenceJson, "preferenceJson");
        try {
            OBJECT_MAPPER.readTree(normalized);
            return normalized;
        } catch (JsonProcessingException ex) {
            throw new PlatformException("page preference preferenceJson must be valid JSON", ex);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PlatformException("page preference " + fieldName + " must not be blank");
        }
        return value.trim();
    }
}
