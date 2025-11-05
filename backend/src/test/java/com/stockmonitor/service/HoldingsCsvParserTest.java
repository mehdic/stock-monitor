package com.stockmonitor.service;

import com.stockmonitor.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit test for CSV holdings parser.
 *
 * Tests:
 * - Valid CSV parsing
 * - Invalid formats (missing columns, wrong types)
 * - Edge cases (empty file, negative quantities, special characters)
 */
public class HoldingsCsvParserTest extends BaseUnitTest {

    @InjectMocks
    private HoldingsCsvParser csvParser;

    @BeforeEach
    public void setup() {
        csvParser = new HoldingsCsvParser();
    }

    @Test
    public void testParseValidCsv() throws Exception {
        String csvContent = """
                ticker,quantity,cost_basis,currency
                AAPL,100,150.50,USD
                MSFT,50,250.75,USD
                GOOGL,25,2500.00,USD
                """;

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holdings.csv",
                "text/csv",
                inputStream
        );

        HoldingsCsvParser.ParseResult result = csvParser.parse(file.getInputStream());

        assertThat(result.getHoldings()).hasSize(3);

        // Verify first holding
        var aapl = result.getHoldings().get(0);
        assertThat(aapl.getTicker()).isEqualTo("AAPL");
        assertThat(aapl.getQuantity()).isEqualTo(100);
        assertThat(aapl.getCostBasis()).isEqualTo(150.50);
        assertThat(aapl.getCurrency()).isEqualTo("USD");

        // Verify second holding
        var msft = result.getHoldings().get(1);
        assertThat(msft.getTicker()).isEqualTo("MSFT");
        assertThat(msft.getQuantity()).isEqualTo(50);
        assertThat(msft.getCostBasis()).isEqualTo(250.75);

        // Verify third holding
        var googl = result.getHoldings().get(2);
        assertThat(googl.getTicker()).isEqualTo("GOOGL");
        assertThat(googl.getQuantity()).isEqualTo(25);
        assertThat(googl.getCostBasis()).isEqualTo(2500.00);
    }

    @Test
    public void testParseWithDifferentCurrencies() throws Exception {
        String csvContent = """
                ticker,quantity,cost_basis,currency
                VOD.L,1000,120.50,GBP
                SAP.DE,100,95.25,EUR
                SONY,50,8500.00,JPY
                """;

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holdings.csv",
                "text/csv",
                inputStream
        );

        HoldingsCsvParser.ParseResult result = csvParser.parse(file.getInputStream());

        assertThat(result.getHoldings()).hasSize(3);
        assertThat(result.getHoldings().get(0).getCurrency()).isEqualTo("GBP");
        assertThat(result.getHoldings().get(1).getCurrency()).isEqualTo("EUR");
        assertThat(result.getHoldings().get(2).getCurrency()).isEqualTo("JPY");
    }

    @Test
    public void testParseMissingColumns() throws Exception {
        String csvContent = """
                ticker,quantity
                AAPL,100
                MSFT,50
                """;

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holdings.csv",
                "text/csv",
                inputStream
        );

        assertThatThrownBy(() -> csvParser.parse(file.getInputStream()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required columns");
    }

    @Test
    public void testParseInvalidQuantity() throws Exception {
        String csvContent = """
                ticker,quantity,cost_basis,currency
                AAPL,invalid,150.50,USD
                """;

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holdings.csv",
                "text/csv",
                inputStream
        );

        HoldingsCsvParser.ParseResult result = csvParser.parse(file.getInputStream());

        assertThat(result.getHoldings()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage()).contains("Invalid quantity");
    }

    @Test
    public void testParseNegativeQuantity() throws Exception {
        String csvContent = """
                ticker,quantity,cost_basis,currency
                AAPL,-100,150.50,USD
                """;

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holdings.csv",
                "text/csv",
                inputStream
        );

        HoldingsCsvParser.ParseResult result = csvParser.parse(file.getInputStream());

        assertThat(result.getHoldings()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage()).contains("Quantity cannot be negative");
    }

    @Test
    public void testParseEmptyFile() throws Exception {
        String csvContent = """
                ticker,quantity,cost_basis,currency
                """;

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holdings.csv",
                "text/csv",
                inputStream
        );

        HoldingsCsvParser.ParseResult result = csvParser.parse(file.getInputStream());

        // File with only header is valid but contains no data
        assertThat(result.getHoldings()).isEmpty();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    public void testParseInvalidSymbol() throws Exception {
        String csvContent = """
                ticker,quantity,cost_basis,currency
                ,100,150.50,USD
                """;

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holdings.csv",
                "text/csv",
                inputStream
        );

        HoldingsCsvParser.ParseResult result = csvParser.parse(file.getInputStream());

        assertThat(result.getHoldings()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage()).contains("Ticker is required");
    }

    @Test
    public void testParseWithWhitespace() throws Exception {
        String csvContent = """
                ticker,quantity,cost_basis,currency
                 AAPL ,100,150.50, USD
                MSFT  ,50,250.75,USD
                """;

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holdings.csv",
                "text/csv",
                inputStream
        );

        HoldingsCsvParser.ParseResult result = csvParser.parse(file.getInputStream());

        assertThat(result.getHoldings()).hasSize(2);
        // Parser should trim whitespace
        assertThat(result.getHoldings().get(0).getTicker()).isEqualTo("AAPL");
        assertThat(result.getHoldings().get(0).getCurrency()).isEqualTo("USD");
    }

    @Test
    public void testParseFractionalQuantity() throws Exception {
        String csvContent = """
                ticker,quantity,cost_basis,currency
                BRK.B,10.5,350.25,USD
                """;

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holdings.csv",
                "text/csv",
                inputStream
        );

        HoldingsCsvParser.ParseResult result = csvParser.parse(file.getInputStream());

        assertThat(result.getHoldings()).hasSize(1);
        assertThat(result.getHoldings().get(0).getQuantity()).isEqualTo(10.5);
    }

    @Test
    public void testParseInvalidCurrency() throws Exception {
        String csvContent = """
                ticker,quantity,cost_basis,currency
                AAPL,100,150.50,INVALID
                """;

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holdings.csv",
                "text/csv",
                inputStream
        );

        HoldingsCsvParser.ParseResult result = csvParser.parse(file.getInputStream());

        assertThat(result.getHoldings()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage()).contains("Invalid currency");
    }
}
