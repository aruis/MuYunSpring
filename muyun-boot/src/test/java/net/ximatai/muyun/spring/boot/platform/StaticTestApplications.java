package net.ximatai.muyun.spring.boot.platform;

/** Shared static application declarations used only by platform contract fixtures. */
public final class StaticTestApplications {
    private StaticTestApplications() {
    }

    @PlatformStaticApplication(alias = "demo", title = "Demo")
    public static class DemoApplication {
    }

    @PlatformStaticApplication(alias = "sales", title = "Sales")
    public static class SalesApplication {
    }
}
