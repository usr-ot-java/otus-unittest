package otus.study.cashmachine.machine.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import otus.study.cashmachine.TestUtil;
import otus.study.cashmachine.bank.dao.CardsDao;
import otus.study.cashmachine.bank.data.Card;
import otus.study.cashmachine.bank.service.AccountService;
import otus.study.cashmachine.bank.service.impl.CardServiceImpl;
import otus.study.cashmachine.machine.data.CashMachine;
import otus.study.cashmachine.machine.data.MoneyBox;
import otus.study.cashmachine.machine.service.impl.CashMachineServiceImpl;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashMachineServiceTest {

    @Spy
    @InjectMocks
    private CardServiceImpl cardService;

    @Mock
    private CardsDao cardsDao;

    @Mock
    private AccountService accountService;

    @Mock
    private MoneyBoxService moneyBoxService;

    private CashMachineServiceImpl cashMachineService;

    private CashMachine cashMachine = new CashMachine(new MoneyBox());

    @BeforeEach
    void init() {
        cashMachineService = new CashMachineServiceImpl(cardService, accountService, moneyBoxService);
    }


    @Test
    void getMoney() {
        doReturn(new BigDecimal(100)).when(cardService).getMoney("1111", "0000", new BigDecimal(100));
        when(moneyBoxService.getMoney(cashMachine.getMoneyBox(), 100)).thenReturn(List.of(1, 0, 0, 0, 0));

        List<Integer> result = cashMachineService.getMoney(cashMachine, "1111", "0000", new BigDecimal(100));
        assertEquals(List.of(1, 0, 0, 0, 0), result);
    }

    @Test
    void getMoneyExceptionally() {
        assertThrows(IllegalArgumentException.class, () ->
                cashMachineService.getMoney(cashMachine, "1111", "0000", new BigDecimal(100))
        );
        verify(cardService, times(1)).putMoney("1111", "0000", new BigDecimal(100));
    }

    @Test
    void putMoney() {
        when(cardsDao.getCardByNumber("1111"))
                .thenReturn(new Card(1, "1111", 2L, TestUtil.getHash("0000")));

        cashMachineService.putMoney(cashMachine, "1111", "0000", List.of(1, 1, 1, 1));

        verify(moneyBoxService, times(1))
                .putMoney(cashMachine.getMoneyBox(), 1, 1, 1, 1);
        verify(accountService, times(1)).putMoney(2L, new BigDecimal(6600));
    }

    @Test
    void checkBalance() {
        when(cardsDao.getCardByNumber("1111"))
                .thenReturn(new Card(1, "1111", 2L, TestUtil.getHash("0000")));
        when(accountService.checkBalance(1L)).thenReturn(new BigDecimal(100));

        BigDecimal result = cashMachineService.checkBalance(cashMachine, "1111", "0000");

        assertEquals(new BigDecimal(100), result);
        verify(accountService, times(1)).checkBalance(1L);
    }

    @Test
    void changePin() {
        ArgumentCaptor<String> cardNumberCaptor1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> oldPinCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> newPinCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cardNumberCaptor2 = ArgumentCaptor.forClass(String.class);

        when(cardsDao.getCardByNumber("1111"))
                .thenReturn(new Card(1L, "1111", 2L, TestUtil.getHash("0000")));

        Boolean result = cashMachineService.changePin("1111", "0000", "1111");
        verify(cardService).cnangePin(cardNumberCaptor1.capture(), oldPinCaptor.capture(), newPinCaptor.capture());
        verify(cardsDao).getCardByNumber(cardNumberCaptor2.capture());

        assertEquals(true, result);
        assertEquals("1111", cardNumberCaptor1.getValue());
        assertEquals("0000", oldPinCaptor.getValue());
        assertEquals("1111", newPinCaptor.getValue());
        assertEquals("1111", cardNumberCaptor2.getValue());
    }

    @Test
    void changePinWithAnswer() {
        when(cardsDao.getCardByNumber("1111"))
                .thenAnswer(invocation -> new Card(1L, invocation.getArgument(0), 2L, TestUtil.getHash("0000")));
        when(cardsDao.saveCard(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Boolean result = cashMachineService.changePin("1111", "0000", "1111");
        assertEquals(true, result);
    }
}