package com.nuaa.recycle.exchange.service;

import com.nuaa.recycle.auth.entity.User;
import com.nuaa.recycle.auth.mapper.RecyclerMapper;
import com.nuaa.recycle.auth.mapper.UserMapper;
import com.nuaa.recycle.catalog.entity.Product;
import com.nuaa.recycle.catalog.mapper.ProductMapper;
import com.nuaa.recycle.catalog.mapper.SiteMapper;
import com.nuaa.recycle.common.auth.CurrentUser;
import com.nuaa.recycle.common.auth.Role;
import com.nuaa.recycle.common.auth.UserContext;
import com.nuaa.recycle.common.error.BizException;
import com.nuaa.recycle.common.error.ErrorCode;
import com.nuaa.recycle.common.util.CodeGenerator;
import com.nuaa.recycle.delivery.mapper.PointsLogMapper;
import com.nuaa.recycle.exchange.dto.ExchangeCreateRequest;
import com.nuaa.recycle.exchange.dto.ExchangeOrderResponse;
import com.nuaa.recycle.exchange.entity.ExchangeOrder;
import com.nuaa.recycle.exchange.mapper.ExchangeOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RecyclerMapper recyclerMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ExchangeOrderMapper exchangeOrderMapper;
    @Mock
    private PointsLogMapper pointsLogMapper;
    @Mock
    private SiteMapper siteMapper;
    @Mock
    private CodeGenerator codeGenerator;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ExchangeService exchangeService;

    @BeforeEach
    void setUp() {
        UserContext.set(new CurrentUser(1L, Role.USER));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void returnsExistingOrderWhenIdempotencyKeyAlreadyUsed() {
        ExchangeOrder existing = order(100L);
        when(exchangeOrderMapper.findByIdempotencyKey(1L, "K1")).thenReturn(existing);
        when(productMapper.selectById(10L)).thenReturn(product(10, 60, "ON", 0));
        when(userMapper.selectById(1L)).thenReturn(user(100));

        ExchangeOrderResponse response = exchangeService.create(new ExchangeCreateRequest(10L, 1, "K1"));

        assertEquals(100L, response.orderId());
        verify(transactionTemplate, never()).execute(any(TransactionCallback.class));
    }

    @Test
    void rejectsWhenPointsInsufficient() {
        stubTransaction();
        when(productMapper.selectById(10L)).thenReturn(product(10, 60, "ON", 0));
        when(userMapper.deductPoints(1L, 60)).thenReturn(0);

        BizException exception = assertThrows(BizException.class,
                () -> exchangeService.create(new ExchangeCreateRequest(10L, 1, "K2")));

        assertEquals(ErrorCode.POINTS_INSUFFICIENT.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void rejectsWhenStockInsufficient() {
        stubTransaction();
        Product before = product(1, 60, "ON", 0);
        Product after = product(0, 60, "ON", 1);
        when(productMapper.selectById(10L)).thenReturn(before, after);
        when(userMapper.deductPoints(1L, 60)).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(user(40));
        when(productMapper.deductStock(10L, 1, 0)).thenReturn(0);

        BizException exception = assertThrows(BizException.class,
                () -> exchangeService.create(new ExchangeCreateRequest(10L, 1, "K3")));

        assertEquals(ErrorCode.STOCK_INSUFFICIENT.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void returnsFirstOrderWhenConcurrentIdempotencyInsertConflicts() {
        ExchangeOrder existing = order(200L);
        when(exchangeOrderMapper.findByIdempotencyKey(1L, "K4")).thenReturn(null, existing);
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'uk_user_idem'"));
        when(productMapper.selectById(10L)).thenReturn(product(10, 60, "ON", 0));
        when(userMapper.selectById(1L)).thenReturn(user(100));

        ExchangeOrderResponse response = exchangeService.create(new ExchangeCreateRequest(10L, 1, "K4"));

        assertEquals(200L, response.orderId());
    }

    private void stubTransaction() {
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(org.mockito.Mockito.mock(TransactionStatus.class));
        });
    }

    private ExchangeOrder order(Long id) {
        ExchangeOrder order = new ExchangeOrder();
        order.setId(id);
        order.setOrderNo("E20261114000001");
        order.setUserId(1L);
        order.setProductId(10L);
        order.setQuantity(1);
        order.setPointsCost(60);
        order.setStatus("PENDING");
        order.setRedeemCode("ABCD2345");
        order.setRedeemExpireAt(LocalDateTime.now().plusDays(30));
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    private Product product(int stock, int price, String status, int version) {
        Product product = new Product();
        product.setId(10L);
        product.setName("测试商品");
        product.setPointsPrice(price);
        product.setStock(stock);
        product.setStatus(status);
        product.setVersion(version);
        return product;
    }

    private User user(int balance) {
        User user = new User();
        user.setId(1L);
        user.setNickname("测试用户");
        user.setPointsBalance(balance);
        return user;
    }
}
