package com.nuaa.recycle.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 契约要求列表接口返回 productId / appealId 等字段，实体 id 必须按契约序列化。
 */
class EntityJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void productSerializesProductId() throws Exception {
        Product product = new Product();
        product.setId(7L);
        product.setName("测试商品");

        String json = objectMapper.writeValueAsString(product);

        assertTrue(json.contains("\"productId\":7"), json);
    }

    @Test
    void appealSerializesAppealId() throws Exception {
        Appeal appeal = new Appeal();
        appeal.setId(9L);
        appeal.setStatus("PENDING");

        String json = objectMapper.writeValueAsString(appeal);

        assertTrue(json.contains("\"appealId\":9"), json);
    }
}
