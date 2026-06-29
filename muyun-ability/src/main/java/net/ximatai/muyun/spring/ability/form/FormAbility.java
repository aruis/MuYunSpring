package net.ximatai.muyun.spring.ability.form;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

public interface FormAbility<T extends EntityContract> {
    FormDescriptor formDescriptor();

    default FormSchema formSchema() {
        Class<?> modelClass = this instanceof CrudAbility<?> crudAbility ? crudAbility.modelClass() : null;
        return FormSchema.from(formDescriptor(), modelClass);
    }
}
