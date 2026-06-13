package net.ximatai.muyun.spring.migration;

import net.ximatai.muyun.spring.common.schema.StaticSchemaService;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Creates the {@code test_person} table and seeds three rows before any migration runs, so the
 * migration under test has real data to transform.
 */
@Component
@Order(10)
public class TestSeedRunner implements ApplicationRunner {

    private final StaticSchemaService schemaService;
    private final TestPersonService personService;

    public TestSeedRunner(StaticSchemaService schemaService, TestPersonService personService) {
        this.schemaService = schemaService;
        this.personService = personService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (TenantContext.Scope ignored = TenantContext.system("test seed")) {
            schemaService.ensureTable(TestPerson.class);
            seed("1", "Alice", 18);
            seed("2", "Bob", 20);
            seed("3", "Carol", 22);
        }
    }

    private void seed(String id, String name, int age) {
        if (personService.select(id) == null) {
            TestPerson person = new TestPerson();
            person.setId(id);
            person.setName(name);
            person.setAge(age);
            personService.insert(person);
        }
    }
}
