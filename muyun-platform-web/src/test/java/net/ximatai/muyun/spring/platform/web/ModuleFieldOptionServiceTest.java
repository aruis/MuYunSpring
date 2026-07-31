package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionField;
import net.ximatai.muyun.spring.common.option.OptionItem;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSource;
import net.ximatai.muyun.spring.common.option.OptionSourceProvider;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.common.option.OptionSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleFieldOptionServiceTest {
    @Test
    void shouldResolveStaticFieldOptionsThroughOptionSourceRegistry() {
        StaticModuleDefinition definition = StaticModuleDefinition.builder("iam", "iam.employee", "职员")
                .modelClass(Employee.class)
                .build();
        StaticModuleDefinitionCatalog catalog = new StaticModuleDefinitionCatalog(List.of(definition));
        OptionSourceRegistry registry = new OptionSourceRegistry(List.of(new GenderOptionProvider()));
        ModuleFieldOptionService service = new ModuleFieldOptionService(catalog,
                new StaticListableBeanFactory().getBeanProvider(net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService.class),
                registry);

        assertThat(service.options("iam.employee", "gender", true, null))
                .extracting(OptionItem::code, OptionItem::title)
                .containsExactly(tuple("1", "男"), tuple("2", "女"));
    }

    private static org.assertj.core.groups.Tuple tuple(String code, String title) {
        return org.assertj.core.groups.Tuple.tuple(code, title);
    }

    private static class Employee {
        @OptionField(type = OptionSourceType.DICTIONARY, source = "iam.gender", titleOutput = net.ximatai.muyun.spring.common.option.OptionTitleOutput.NONE)
        private String gender;
    }

    private static class GenderOptionProvider implements OptionSourceProvider {
        @Override
        public String sourceType() {
            return OptionBinding.DICTIONARY_SOURCE;
        }

        @Override
        public OptionSource source(OptionBinding binding) {
            return new OptionSource() {
                @Override
                public OptionBinding binding() {
                    return binding;
                }

                @Override
                public List<OptionItem> options(OptionQuery query) {
                    return List.of(new OptionItem("1", "男", true, 10, null),
                            new OptionItem("2", "女", true, 20, null));
                }
            };
        }
    }
}
