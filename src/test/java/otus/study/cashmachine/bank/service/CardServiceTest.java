package otus.study.cashmachine.bank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import otus.study.cashmachine.TestUtil;
import otus.study.cashmachine.bank.dao.CardsDao;
import otus.study.cashmachine.bank.data.Card;
import otus.study.cashmachine.bank.service.impl.CardServiceImpl;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CardServiceTest {
    AccountService accountService;

    CardsDao cardsDao;

    CardService cardService;

    @BeforeEach
    void init() {
        cardsDao = mock(CardsDao.class);
        accountService = mock(AccountService.class);
        cardService = new CardServiceImpl(accountService, cardsDao);
    }

    @Test
    void testCreateCard() {
        when(cardsDao.createCard("5555", 1L, "0123")).thenReturn(
                new Card(1L, "5555", 1L, "0123"));

        Card newCard = cardService.createCard("5555", 1L, "0123");
        assertNotEquals(0, newCard.getId());
        assertEquals("5555", newCard.getNumber());
        assertEquals(1L, newCard.getAccountId());
        assertEquals("0123", newCard.getPinCode());
    }

    @Test
    void checkBalanceExceptionally() {
        assertThrows(IllegalArgumentException.class,
                () -> cardService.getBalance("1234", "0000")
        );
    }

    @Test
    void checkBalance() {
        Card card = new Card(1L, "1234", 1L, TestUtil.getHash("0000"));
        when(cardsDao.getCardByNumber(anyString())).thenReturn(card);
        when(accountService.checkBalance(1L)).thenReturn(new BigDecimal(1000));

        BigDecimal sum = cardService.getBalance("1234", "0000");
        assertEquals(0, sum.compareTo(new BigDecimal(1000)));
    }

    @Test
    void getMoneyExceptionally() {
        assertThrows(IllegalArgumentException.class,
                () -> cardService.getMoney("1111", "0000", BigDecimal.ONE)
        );
    }

    @Test
    void getMoney() {
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);

        when(cardsDao.getCardByNumber("1111"))
                .thenReturn(new Card(1L, "1111", 100L, TestUtil.getHash("0000")));

        when(accountService.getMoney(idCaptor.capture(), amountCaptor.capture()))
                .thenReturn(BigDecimal.TEN);

        cardService.getMoney("1111", "0000", BigDecimal.ONE);

        verify(accountService, only()).getMoney(anyLong(), any());
        assertEquals(BigDecimal.ONE, amountCaptor.getValue());
        assertEquals(100L, idCaptor.getValue().longValue());
    }

    @Test
    void putMoneyExceptionally() {
        assertThrows(IllegalArgumentException.class, () ->
                cardService.putMoney("1111", "0000", new BigDecimal(100)));
    }

    @Test
    void putMoney() {
        ArgumentCaptor<Long> accountIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<BigDecimal> sumCaptor = ArgumentCaptor.forClass(BigDecimal.class);

        when(cardsDao.getCardByNumber("1111"))
                .thenReturn(new Card(1L, "1111", 2L, TestUtil.getHash("0000")));
        when(accountService.putMoney(anyLong(), any())).thenReturn(new BigDecimal(100));

        BigDecimal result = cardService.putMoney("1111", "0000", new BigDecimal(100));
        verify(accountService).putMoney(accountIdCaptor.capture(), sumCaptor.capture());

        assertEquals(new BigDecimal(100), result);
        assertEquals(2L, accountIdCaptor.getValue());
        assertEquals(new BigDecimal(100), sumCaptor.getValue());
    }

    @Test
    void checkIncorrectPin() {
        Card card = new Card(1L, "1234", 1L, "0000");
        when(cardsDao.getCardByNumber(eq("1234"))).thenReturn(card);

        Exception thrown = assertThrows(IllegalArgumentException.class, () -> {
            cardService.getBalance("1234", "0012");
        });
        assertEquals(thrown.getMessage(), "Pincode is incorrect");
    }

    @Test
    void changePinExceptionallyWhenCardDoesNotExist() {
        assertThrows(IllegalArgumentException.class,
                () -> cardService.cnangePin("1111", "1234", "4321")
        );
    }

    @Test
    void changePinExceptionallyWhenCardErrorSaving() {
        when(cardsDao.getCardByNumber("1111"))
                .thenReturn(new Card(1L, "1111", 2L, TestUtil.getHash("0000")));
        when(cardsDao.saveCard(any())).thenThrow(new RuntimeException("error"));

        Boolean result = cardService.cnangePin("1111", "0000", "5555");
        assertEquals(false, result);
    }

    @Test
    void changePin() {
        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        when(cardsDao.getCardByNumber("1111"))
                .thenReturn(new Card(1L, "1111", 2L, TestUtil.getHash("0000")));

        Boolean result = cardService.cnangePin("1111", "0000", "5555");
        verify(cardsDao).saveCard(cardCaptor.capture());

        assertEquals(true, result);

        Card expected = new Card(1L, "1111", 2L, TestUtil.getHash("5555"));
        assertEquals(expected.getAccountId(), cardCaptor.getValue().getAccountId());
        assertEquals(expected.getId(), cardCaptor.getValue().getId());
        assertEquals(expected.getNumber(), cardCaptor.getValue().getNumber());
        assertEquals(expected.getPinCode(), cardCaptor.getValue().getPinCode());
    }
}