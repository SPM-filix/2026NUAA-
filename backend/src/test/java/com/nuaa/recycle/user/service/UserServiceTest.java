package com.nuaa.recycle.user.service;

import com.nuaa.recycle.auth.entity.User;
import com.nuaa.recycle.auth.mapper.RecyclerMapper;
import com.nuaa.recycle.auth.mapper.UserMapper;
import com.nuaa.recycle.catalog.mapper.ProductMapper;
import com.nuaa.recycle.catalog.mapper.RecycleCategoryMapper;
import com.nuaa.recycle.catalog.mapper.SiteMapper;
import com.nuaa.recycle.common.auth.CurrentUser;
import com.nuaa.recycle.common.auth.JwtService;
import com.nuaa.recycle.common.auth.Role;
import com.nuaa.recycle.common.auth.UserContext;
import com.nuaa.recycle.delivery.mapper.DeliveryRecordMapper;
import com.nuaa.recycle.delivery.mapper.PointsLogMapper;
import com.nuaa.recycle.exchange.mapper.ExchangeOrderMapper;
import com.nuaa.recycle.user.dto.UserProfileResponse;
import com.nuaa.recycle.user.dto.UserUpdateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RecyclerMapper recyclerMapper;
    @Mock
    private DeliveryRecordMapper deliveryRecordMapper;
    @Mock
    private PointsLogMapper pointsLogMapper;
    @Mock
    private ExchangeOrderMapper exchangeOrderMapper;
    @Mock
    private RecycleCategoryMapper categoryMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private SiteMapper siteMapper;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        UserContext.set(new CurrentUser(1L, Role.USER));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void updateProfileOnlyTouchesEditableFields() {
        User user = new User();
        user.setId(1L);
        user.setUserCode("123456");
        user.setPhone("13800001234");
        user.setNickname("旧昵称");
        user.setAddress("老地址");
        user.setPointsBalance(30);
        user.setStatus("ACTIVE");
        when(userMapper.selectById(1L)).thenReturn(user);

        UserProfileResponse response = userService.updateProfile(new UserUpdateRequest("新昵称", null, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User saved = captor.getValue();
        assertEquals("新昵称", saved.getNickname());
        assertEquals("老地址", saved.getAddress());
        assertEquals("13800001234", saved.getPhone());
        assertEquals(30, saved.getPointsBalance());
        assertEquals("新昵称", response.nickname());
        assertEquals("13800001234", response.phone());
        assertEquals(30, response.pointsBalance());
        assertNull(response.avatarUrl());
    }
}
