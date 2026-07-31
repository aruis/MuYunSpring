package net.ximatai.muyun.spring.demo.school.configuration;

import net.ximatai.muyun.spring.platform.application.PlatformStaticApplication;

/** 教学管理演示应用的稳定身份；不承载演示 Bean 装配。 */
@PlatformStaticApplication(alias = "education", title = "教学管理", sortOrder = 100)
public class EducationApplication {
}
