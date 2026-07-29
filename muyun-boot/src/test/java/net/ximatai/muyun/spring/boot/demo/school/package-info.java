/**
 * 可运行的测试型静态业务示例，用于演示平台通用能力如何在最终 Boot 交付物中组合。
 *
 * <p>示例覆盖普通校园领域：爱好分类树、学生主数据、通过平台数据字典管理教学学科的教师主数据，
 * 以及包含有序成员子表的班级聚合。各领域包内的 Controller 只组合 {@code CrudWeb} 与必要的 Web
 * 投影策略，标准 Ability 端点继续由平台统一装配。{@link
 * TeachingDemoIT} 用集成测试锁定这些 Ability
 * 组合提供的关键行为边界。</p>
 */
package net.ximatai.muyun.spring.boot.demo.school;

import net.ximatai.muyun.spring.boot.demo.school.test.TeachingDemoIT;
