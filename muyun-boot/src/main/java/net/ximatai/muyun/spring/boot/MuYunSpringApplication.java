package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.spring.boot.sql.annotation.EnableMuYunRepositories;
import net.ximatai.muyun.spring.iam.organization.OrganizationDao;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "net.ximatai.muyun.spring")
@EnableMuYunRepositories(basePackageClasses = OrganizationDao.class)
@EnableScheduling
public class MuYunSpringApplication {
    public static void main(String[] args) {
        SpringApplication.run(MuYunSpringApplication.class, args);
    }
}
