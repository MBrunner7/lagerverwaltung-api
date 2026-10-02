package de.mbrunner.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the HTTP layer against a real PostgreSQL (Testcontainers), including
 * the Flyway migrations. Every test creates its own products and locations with unique keys,
 * so the tests are independent of each other and of execution order.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InventoryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullGoodsFlowReceiptTransferIssue() throws Exception {
        long product = createProduct(unique("BOLT"), 0);
        long shelfA = createLocation(unique("A"));
        long shelfB = createLocation(unique("B"));

        postJson("/api/movements/receipts", """
                {"productId": %d, "locationId": %d, "quantity": 100, "reference": "PO-4711"}
                """.formatted(product, shelfA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("RECEIPT"))
                .andExpect(jsonPath("$.reference").value("PO-4711"));

        postJson("/api/movements/transfers", """
                {"productId": %d, "fromLocationId": %d, "toLocationId": %d, "quantity": 30}
                """.formatted(product, shelfA, shelfB))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TRANSFER"));

        postJson("/api/movements/issues", """
                {"productId": %d, "locationId": %d, "quantity": 25}
                """.formatted(product, shelfB))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/stock").param("productId", String.valueOf(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.locationId == %d)].quantity".formatted(shelfA)).value(70))
                .andExpect(jsonPath("$[?(@.locationId == %d)].quantity".formatted(shelfB)).value(5));

        mockMvc.perform(get("/api/movements").param("productId", String.valueOf(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].type").value("ISSUE"))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void issuingMoreThanAvailableReturns409AndChangesNothing() throws Exception {
        long product = createProduct(unique("NUT"), 0);
        long shelf = createLocation(unique("C"));
        receive(product, shelf, 5);

        postJson("/api/movements/issues", """
                {"productId": %d, "locationId": %d, "quantity": 6}
                """.formatted(product, shelf))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient stock"))
                .andExpect(jsonPath("$.available").value(5))
                .andExpect(jsonPath("$.requested").value(6));

        mockMvc.perform(get("/api/stock").param("productId", String.valueOf(product)))
                .andExpect(jsonPath("$[0].quantity").value(5));
    }

    @Test
    void lowStockReportListsProductsBelowReorderPoint() throws Exception {
        String lowSku = unique("LOW");
        String okSku = unique("OK");
        long low = createProduct(lowSku, 50);
        long ok = createProduct(okSku, 10);
        long shelf = createLocation(unique("D"));
        receive(low, shelf, 20);
        receive(ok, shelf, 10);

        mockMvc.perform(get("/api/stock/low"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", hasItem(lowSku)))
                .andExpect(jsonPath("$[?(@.sku == '%s')].shortfall".formatted(lowSku)).value(30))
                .andExpect(jsonPath("$[?(@.sku == '%s')]".formatted(okSku), hasSize(0)));
    }

    @Test
    void invalidRequestReturnsFieldErrors() throws Exception {
        postJson("/api/products", """
                {"sku": "", "name": "Bolt", "unit": "PCS", "minStock": -1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.sku").exists())
                .andExpect(jsonPath("$.errors.minStock").exists());
    }

    @Test
    void duplicateSkuReturns409() throws Exception {
        String sku = unique("DUP");
        createProduct(sku, 0);

        postJson("/api/products", productJson(sku, 0))
                .andExpect(status().isConflict());
    }

    @Test
    void productCanBeUpdatedButKeepsItsSku() throws Exception {
        String sku = unique("UPD");
        long product = createProduct(sku, 5);

        mockMvc.perform(put("/api/products/{id}", product)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Hex bolt M8", "unit": "pcs", "minStock": 40}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.name").value("Hex bolt M8"))
                .andExpect(jsonPath("$.unit").value("PCS"))
                .andExpect(jsonPath("$.minStock").value(40));
    }

    @Test
    void transferToSameLocationReturns422() throws Exception {
        long product = createProduct(unique("SAME"), 0);
        long shelf = createLocation(unique("E"));

        postJson("/api/movements/transfers", """
                {"productId": %d, "fromLocationId": %d, "toLocationId": %d, "quantity": 1}
                """.formatted(product, shelf, shelf))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unknownProductReturns404() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product with id 999999 not found"));
    }

    @Test
    void stockQueryNeedsExactlyOneFilter() throws Exception {
        mockMvc.perform(get("/api/stock"))
                .andExpect(status().isBadRequest());
    }

    // --- helpers -------------------------------------------------------------------------------

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String productJson(String sku, int minStock) {
        return """
                {"sku": "%s", "name": "Test product", "unit": "PCS", "minStock": %d}
                """.formatted(sku, minStock);
    }

    private long createProduct(String sku, int minStock) throws Exception {
        String body = postJson("/api/products", productJson(sku, minStock))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    private long createLocation(String code) throws Exception {
        String body = postJson("/api/locations", """
                {"code": "%s", "name": "Test location"}
                """.formatted(code))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    private void receive(long product, long location, int quantity) throws Exception {
        postJson("/api/movements/receipts", """
                {"productId": %d, "locationId": %d, "quantity": %d}
                """.formatted(product, location, quantity))
                .andExpect(status().isCreated());
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long idOf(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asLong();
    }
}
