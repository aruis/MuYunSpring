package net.ximatai.muyun.spring.platform.exchange.protocol;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.exchange.model.ParsedColumn;
import net.ximatai.muyun.spring.platform.exchange.model.ParsedSheet;
import net.ximatai.muyun.spring.platform.exchange.model.ParsedWorkbook;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelExchangeSheetResolverTest {
    private final ExcelExchangeSheetResolver resolver = new ExcelExchangeSheetResolver();

    @Test
    void shouldResolveMainAndChildSheets() {
        ParsedSheet main = sheet("Order", "order");
        ParsedSheet child = sheet("Order Line", "orderLine");

        ExcelExchangeSheetResolver.ResolvedSheets resolved = resolver.resolve(
                new ParsedWorkbook(List.of(main, child)),
                "order",
                Set.of("orderLine")
        );

        assertThat(resolved.mainSheet()).isSameAs(main);
        assertThat(resolved.childSheets()).containsEntry("orderLine", child);
        assertThat(resolved.childSheetList()).containsExactly(child);
    }

    @Test
    void shouldRejectDuplicateMainSheet() {
        ParsedWorkbook workbook = new ParsedWorkbook(List.of(
                sheet("Order A", "order"),
                sheet("Order B", "order")
        ));

        assertThatThrownBy(() -> resolver.resolve(workbook, "order", Set.of()))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("main sheet duplicated");
    }

    @Test
    void shouldRejectDuplicateChildSheet() {
        ParsedWorkbook workbook = new ParsedWorkbook(List.of(
                sheet("Order", "order"),
                sheet("Line A", "orderLine"),
                sheet("Line B", "orderLine")
        ));

        assertThatThrownBy(() -> resolver.resolve(workbook, "order", Set.of("orderLine")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("child sheet duplicated");
    }

    @Test
    void shouldRejectMissingMainSheet() {
        ParsedWorkbook workbook = new ParsedWorkbook(List.of(sheet("Order Line", "orderLine")));

        assertThatThrownBy(() -> resolver.resolve(workbook, "order", Set.of("orderLine")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("main sheet not found");
    }

    private ParsedSheet sheet(String sheetName, String entityAlias) {
        return new ParsedSheet(sheetName, entityAlias, List.of(
                new ParsedColumn(0, null, ExcelExchangeProtocol.RELATE_ID_FIELD, ExcelExchangeProtocol.RELATE_ID_TITLE),
                new ParsedColumn(1, entityAlias, "code", "Code")
        ), List.of());
    }
}
