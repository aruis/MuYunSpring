package net.ximatai.muyun.spring.migration;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Migration under test. Steps are deliberately declared out of version order to verify the
 * executor sorts them; step 1 mutates seeded rows, step 2 inserts a new row.
 */
@Component
public class TestMigration extends AbstractMigration {

    private final TestPersonService personService;

    public TestMigration(TestPersonService personService) {
        this.personService = personService;
    }

    @Override
    public String getAlias() {
        return "test-person";
    }

    @Override
    public List<MigrateStep> getMigrateSteps() {
        return List.of(
                new MigrateStep(2, () -> {
                    TestPerson person = new TestPerson();
                    person.setId("4");
                    person.setName("Dave");
                    person.setAge(30);
                    personService.insert(person);
                }),
                new MigrateStep(1, () -> {
                    for (TestPerson person : personService.list(Criteria.of(), PageRequest.of(1, 1000))) {
                        if (person.getAge() != null) {
                            person.setAge(person.getAge() + 10);
                            personService.update(person);
                        }
                    }
                })
        );
    }
}
