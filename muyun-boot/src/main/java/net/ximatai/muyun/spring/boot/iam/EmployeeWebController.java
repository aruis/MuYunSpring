package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.iam.employee.Employee;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.employee", title = "职员管理")
@RequestMapping("/iam.employee")
public class EmployeeWebController extends WebSupport<EmployeeService> implements
        CrudWeb<Employee, EmployeeService>,
        EnableWeb<Employee, EmployeeService>,
        SortWeb<Employee, EmployeeService> {
}
