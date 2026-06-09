package net.ximatai.muyun.spring.platform.exchange.writer;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelColumnPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelSheetPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelValueType;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookMeta;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.protocol.ExcelExchangeProtocol;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelWorkbookPlanWriterTest {
    private final ExcelWorkbookPlanWriter writer = new ExcelWorkbookPlanWriter();

    @Test
    void shouldWriteWorkbookPlanToXlsxBytes() throws IOException {
        ExcelWorkbookPlan plan = new ExcelWorkbookPlan(
                new ExcelWorkbookMeta("1", "sales.order", "order-export", "Order Export", "Asia/Shanghai"),
                List.of(
                        new ExcelSheetPlan(
                                "Order",
                                "order_main",
                                true,
                                List.of(
                                        new ExcelColumnPlan("relateId", "关联标识"),
                                        new ExcelColumnPlan("orderNo", "Order No", true),
                                        new ExcelColumnPlan("amount", "Amount", ExcelValueType.NUMBER),
                                        new ExcelColumnPlan("enabled", "Enabled", ExcelValueType.BOOLEAN),
                                        new ExcelColumnPlan("signedDate", "Signed Date", ExcelValueType.DATE),
                                        new ExcelColumnPlan("submittedAt", "Submitted At", ExcelValueType.DATE_TIME),
                                        new ExcelColumnPlan("remark", "Remark")
                                ),
                                List.of(List.of(
                                        "R-1",
                                        "SO-001",
                                        12.5,
                                        true,
                                        LocalDate.parse("2026-06-08"),
                                        LocalDateTime.parse("2026-06-08T09:30:00"),
                                        "first order"
                                ))
                        ),
                        new ExcelSheetPlan(
                                "Order Line",
                                "order_line",
                                false,
                                List.of(
                                        new ExcelColumnPlan("relateId", "关联标识"),
                                        new ExcelColumnPlan("sku", "SKU"),
                                        new ExcelColumnPlan("qty", "Qty", ExcelValueType.NUMBER)
                                ),
                                List.of(List.of("R-1", "SKU-01", 2))
                        )
                )
        );

        byte[] bytes = writer.writeToBytes(plan);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheet("Order")).isNotNull();
            assertThat(workbook.getSheet("Order Line")).isNotNull();
            assertThat(workbook.getSheet(ExcelExchangeProtocol.META_SHEET_NAME)).isNotNull();
            assertThat(workbook.isSheetHidden(workbook.getSheetIndex(ExcelExchangeProtocol.META_SHEET_NAME))).isTrue();

            Sheet order = workbook.getSheet("Order");
            assertThat(text(order, 0, 0)).isEqualTo("__relateId");
            assertThat(text(order, 0, 1)).isEqualTo("order_main.orderNo");
            assertThat(text(order, 0, 2)).isEqualTo("order_main.amount");
            assertThat(text(order, 0, 3)).isEqualTo("order_main.enabled");
            assertThat(text(order, 0, 4)).isEqualTo("order_main.signedDate");
            assertThat(text(order, 0, 5)).isEqualTo("order_main.submittedAt");
            assertThat(text(order, 1, 0)).isEqualTo("关联标识");
            assertThat(text(order, 1, 1)).isEqualTo("*Order No");
            assertThat(text(order, 1, 2)).isEqualTo("Amount");

            Row data = order.getRow(2);
            assertThat(data.getCell(0).getStringCellValue()).isEqualTo("R-1");
            assertThat(data.getCell(1).getStringCellValue()).isEqualTo("SO-001");
            assertThat(data.getCell(2).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(data.getCell(2).getNumericCellValue()).isEqualTo(12.5);
            assertThat(data.getCell(3).getCellType()).isEqualTo(CellType.BOOLEAN);
            assertThat(data.getCell(3).getBooleanCellValue()).isTrue();
            assertThat(data.getCell(4).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(data.getCell(4).getCellStyle().getDataFormatString()).isEqualTo("yyyy-mm-dd");
            assertThat(data.getCell(5).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(data.getCell(5).getCellStyle().getDataFormatString()).isEqualTo("yyyy-mm-dd hh:mm:ss");
            assertThat(data.getCell(6).getStringCellValue()).isEqualTo("first order");

            Sheet line = workbook.getSheet("Order Line");
            assertThat(text(line, 0, 0)).isEqualTo("__relateId");
            assertThat(text(line, 0, 1)).isEqualTo("order_line.sku");
            assertThat(text(line, 2, 0)).isEqualTo("R-1");
            assertThat(line.getRow(2).getCell(2).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(line.getRow(2).getCell(2).getNumericCellValue()).isEqualTo(2.0);

            Sheet meta = workbook.getSheet(ExcelExchangeProtocol.META_SHEET_NAME);
            assertMetaRow(meta, 0, "protocolVersion", "1");
            assertMetaRow(meta, 1, "moduleAlias", "sales.order");
            assertMetaRow(meta, 2, "uiConfigId", "order-export");
            assertMetaRow(meta, 3, "uiConfigTitle", "Order Export");
            assertMetaRow(meta, 4, "timeZone", "Asia/Shanghai");
        }
    }

    @Test
    void shouldRejectInvalidWorkbookPlanBeforeWriting() {
        ExcelWorkbookPlan plan = new ExcelWorkbookPlan(List.of(
                new ExcelSheetPlan(
                        "Order",
                        "order_main",
                        false,
                        List.of(
                                new ExcelColumnPlan("relateId", "关联标识"),
                                new ExcelColumnPlan("orderNo", "Order No")
                        )
                )
        ));

        assertThatThrownBy(() -> writer.writeToBytes(plan))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("exactly one main sheet");
    }

    @Test
    void shouldWriteDropdownOptionsToHiddenSheetAndDataValidation() throws IOException {
        ExcelWorkbookPlan plan = new ExcelWorkbookPlan(
                new ExcelWorkbookMeta("1", "sales.order", null, null, null),
                List.of(new ExcelSheetPlan(
                        "Order",
                        "order_main",
                        true,
                        List.of(
                                new ExcelColumnPlan("relateId", "关联标识"),
                                new ExcelColumnPlan("status", "Status", false, ExcelValueType.TEXT,
                                        List.of("draft", "active"))
                        )
                ))
        );

        byte[] bytes = writer.writeToBytes(plan);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet options = workbook.getSheet(ExcelExchangeProtocol.OPTIONS_SHEET_NAME);
            assertThat(options).isNotNull();
            assertThat(workbook.isSheetHidden(workbook.getSheetIndex(options))).isTrue();
            assertThat(text(options, 0, 0)).isEqualTo("order_main.status");
            assertThat(text(options, 1, 0)).isEqualTo("draft");
            assertThat(text(options, 2, 0)).isEqualTo("active");

            Sheet order = workbook.getSheet("Order");
            assertThat(order.getDataValidations()).hasSize(1);
        }
    }

    private String text(Sheet sheet, int rowIndex, int columnIndex) {
        return sheet.getRow(rowIndex).getCell(columnIndex).getStringCellValue();
    }

    private void assertMetaRow(Sheet sheet, int rowIndex, String key, String value) {
        Row row = sheet.getRow(rowIndex);
        assertThat(row.getCell(0).getStringCellValue()).isEqualTo(key);
        assertThat(row.getCell(1).getStringCellValue()).isEqualTo(value);
    }
}
