package otus.study.cashmachine.bank.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import otus.study.cashmachine.bank.dao.AccountDao;
import otus.study.cashmachine.bank.data.Account;
import otus.study.cashmachine.bank.service.impl.AccountServiceImpl;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    AccountDao accountDao;

    AccountServiceImpl accountServiceImpl;

    @BeforeEach
    public void beforeEach() {
        accountServiceImpl = new AccountServiceImpl(accountDao);
    }

    @Test
    void createAccountMock() {
        Mockito.when(accountDao.saveAccount(ArgumentMatchers.any()))
                .thenReturn(new Account(1L, new BigDecimal(100)));
        Account result = accountServiceImpl.createAccount(new BigDecimal(100));
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(new BigDecimal(100), result.getAmount());
    }

    @Test
    void createAccountCaptor() {
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        accountServiceImpl.createAccount(new BigDecimal(1000));
        Mockito.verify(accountDao).saveAccount(captor.capture());
        assertEquals(new Account(0, new BigDecimal(1000)), captor.getValue());
    }

    @Test
    void addSum() {
        Mockito.when(accountDao.getAccount(1L))
                .thenReturn(new Account(1L, new BigDecimal(500)));
        BigDecimal result = accountServiceImpl.putMoney(1L, new BigDecimal(100));
        assertEquals(new BigDecimal(600), result);
    }

    @Test
    void getSumExceptionally() {
        Mockito.when(accountDao.getAccount(1L))
                .thenReturn(new Account(1L, new BigDecimal(100)));
        // Try to get more money than account has
        assertThrows(IllegalArgumentException.class, () -> {
            accountServiceImpl.getMoney(1L, new BigDecimal(101));
        });
    }

    @Test
    void getSum() {
        Account account = new Account(1L, new BigDecimal(100));
        Mockito.when(accountDao.getAccount(1L)).thenReturn(account);
        // Try to get less money than account has
        BigDecimal result = accountServiceImpl.getMoney(1L, new BigDecimal(50));
        assertEquals(new BigDecimal(50), result);
        assertEquals(new BigDecimal(50), account.getAmount());
    }

    @Test
    void getAccount() {
        Mockito.when(accountDao.getAccount(1L)).thenReturn(new Account(1L, new BigDecimal(1000)));
        Account account = accountServiceImpl.getAccount(1L);
        assertEquals(1L, account.getId());
        assertEquals(new BigDecimal(1000), account.getAmount());
    }

    @Test
    void checkBalance() {
        Mockito.when(accountDao.getAccount(1L)).thenReturn(new Account(1L, new BigDecimal(100)));
        BigDecimal balance = accountServiceImpl.checkBalance(1L);
        assertEquals(new BigDecimal(100), balance);
    }
}
