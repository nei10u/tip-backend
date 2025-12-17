package com.nei10u.tip.service;

import com.nei10u.tip.dto.UserDto;
import com.nei10u.tip.mapper.UserMapper;
import com.nei10u.tip.model.User;
import com.nei10u.tip.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void testRegister_NewUser() {
        User user = new User();
        user.setUnionId("test_union_id");
        user.setPhone("13800000000");

        when(userMapper.getUserByUnionId("test_union_id")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        UserDto result = userService.register(user);

        assertNotNull(result);
        assertEquals("test_union_id", result.getUnionId());
        verify(userMapper, times(1)).insert(any(User.class));
    }
}
