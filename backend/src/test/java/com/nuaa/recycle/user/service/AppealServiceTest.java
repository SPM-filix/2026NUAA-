package com.nuaa.recycle.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nuaa.recycle.common.api.PageResult;
import com.nuaa.recycle.common.auth.CurrentUser;
import com.nuaa.recycle.common.auth.Role;
import com.nuaa.recycle.common.auth.UserContext;
import com.nuaa.recycle.common.error.BizException;
import com.nuaa.recycle.common.error.ErrorCode;
import com.nuaa.recycle.delivery.entity.DeliveryRecord;
import com.nuaa.recycle.delivery.mapper.DeliveryRecordMapper;
import com.nuaa.recycle.exchange.entity.ExchangeOrder;
import com.nuaa.recycle.exchange.mapper.ExchangeOrderMapper;
import com.nuaa.recycle.user.dto.AppealCreateRequest;
import com.nuaa.recycle.user.dto.AppealCreateResponse;
import com.nuaa.recycle.user.dto.AppealResponse;
import com.nuaa.recycle.user.entity.Appeal;
import com.nuaa.recycle.user.mapper.AppealMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppealServiceTest {

    @Mock
    private AppealMapper appealMapper;
    @Mock
    private DeliveryRecordMapper deliveryRecordMapper;
    @Mock
    private ExchangeOrderMapper exchangeOrderMapper;

    @InjectMocks
    private AppealService appealService;

    @BeforeEach
    void setUp() {
        UserContext.set(new CurrentUser(1L, Role.USER));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createsPendingAppealForOwnDelivery() {
        when(deliveryRecordMapper.selectById(5L)).thenReturn(delivery(5L, 1L));
        doAnswer(invocation -> {
            Appeal appeal = invocation.getArgument(0);
            appeal.setId(99L);
            return 1;
        }).when(appealMapper).insert(any(Appeal.class));

        AppealCreateResponse response = appealService.create(
                new AppealCreateRequest("DELIVERY", 5L, "重量录入有误，请复核该笔流水"));

        assertEquals(99L, response.appealId());
        assertEquals("PENDING", response.status());
        ArgumentCaptor<Appeal> captor = ArgumentCaptor.forClass(Appeal.class);
        verify(appealMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals(5L, captor.getValue().getTargetId());
        assertEquals("PENDING", captor.getValue().getStatus());
    }

    @Test
    void rejectsAppealForOtherUsersDelivery() {
        when(deliveryRecordMapper.selectById(5L)).thenReturn(delivery(5L, 2L));

        BizException exception = assertThrows(BizException.class, () -> appealService.create(
                new AppealCreateRequest("DELIVERY", 5L, "重量录入有误，请复核该笔流水")));

        assertEquals(ErrorCode.FORBIDDEN.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void rejectsAppealForMissingOrder() {
        when(exchangeOrderMapper.selectById(7L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> appealService.create(
                new AppealCreateRequest("EXCHANGE", 7L, "兑换核销后未拿到对应的实物商品")));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void listsOwnAppealsWithTargetNo() {
        Appeal appeal = new Appeal();
        appeal.setId(3L);
        appeal.setUserId(1L);
        appeal.setTargetType("EXCHANGE");
        appeal.setTargetId(7L);
        appeal.setDescription("兑换核销后未拿到对应的实物商品");
        appeal.setStatus("PENDING");
        appeal.setCreatedAt(LocalDateTime.now());
        Page<Appeal> page = new Page<>(1, 10);
        page.setRecords(List.of(appeal));
        page.setTotal(1);
        doReturn(page).when(appealMapper).selectPage(any(), any());

        ExchangeOrder order = new ExchangeOrder();
        order.setId(7L);
        order.setUserId(1L);
        order.setOrderNo("E20261114000001");
        when(exchangeOrderMapper.selectById(7L)).thenReturn(order);

        PageResult<AppealResponse> result = appealService.myAppeals(1, 10);

        assertEquals(1, result.total());
        assertEquals(1, result.list().size());
        assertEquals("E20261114000001", result.list().get(0).targetNo());
    }

    private DeliveryRecord delivery(Long id, Long userId) {
        DeliveryRecord record = new DeliveryRecord();
        record.setId(id);
        record.setUserId(userId);
        record.setRecordNo("D202611140001");
        return record;
    }
}
