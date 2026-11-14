package com.nuaa.recycle.delivery.service;

import com.nuaa.recycle.auth.entity.User;
import com.nuaa.recycle.auth.mapper.RecyclerMapper;
import com.nuaa.recycle.auth.mapper.UserMapper;
import com.nuaa.recycle.catalog.mapper.PointsRuleMapper;
import com.nuaa.recycle.catalog.mapper.RecycleCategoryMapper;
import com.nuaa.recycle.catalog.mapper.SiteMapper;
import com.nuaa.recycle.common.auth.JwtService;
import com.nuaa.recycle.common.error.BizException;
import com.nuaa.recycle.common.error.ErrorCode;
import com.nuaa.recycle.common.util.CodeGenerator;
import com.nuaa.recycle.delivery.mapper.DeliveryRecordMapper;
import com.nuaa.recycle.delivery.mapper.PointsLogMapper;
import com.nuaa.recycle.user.dto.UserLookupResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RecyclerMapper recyclerMapper;
    @Mock
    private RecycleCategoryMapper categoryMapper;
    @Mock
    private PointsRuleMapper pointsRuleMapper;
    @Mock
    private SiteMapper siteMapper;
    @Mock
    private DeliveryRecordMapper deliveryRecordMapper;
    @Mock
    private PointsLogMapper pointsLogMapper;
    @Mock
    private JwtService jwtService;
    @Mock
    private CodeGenerator codeGenerator;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    void lookupByPhoneMasksPhone() {
        User user = new User();
        user.setId(1L);
        user.setUserCode("123456");
        user.setNickname("小魏");
        user.setPhone("13800001234");
        user.setPointsBalance(20);
        user.setStatus("ACTIVE");
        when(userMapper.selectOne(any())).thenReturn(user);

        UserLookupResponse response = deliveryService.lookup("13800001234", null);

        assertEquals("138****1234", response.phoneMasked());
        assertEquals(20, response.pointsBalance());
    }

    @Test
    void lookupRejectsInvalidKeyword() {
        BizException exception = assertThrows(BizException.class,
                () -> deliveryService.lookup("abc", null));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void lookupRejectsUnknownUser() {
        when(userMapper.selectOne(any())).thenReturn(null);

        BizException exception = assertThrows(BizException.class,
                () -> deliveryService.lookup("13800001234", null));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }
}
