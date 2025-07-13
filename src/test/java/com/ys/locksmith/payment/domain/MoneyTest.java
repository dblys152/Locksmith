package com.ys.locksmith.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("금액 값 객체 테스트")
class MoneyTest {
    
    @Test
    @DisplayName("유효한 금액으로 Money를 생성할 수 있다")
    void createValidMoney() {
        // given
        BigDecimal amount = new BigDecimal("1000.50");
        String currency = "USD";
        
        // when
        Money money = Money.of(amount, currency);
        
        // then
        assertThat(money.getAmount()).isEqualTo(new BigDecimal("1000.50"));
        assertThat(money.getCurrency()).isEqualTo("USD");
    }
    
    @Test
    @DisplayName("원화 금액을 생성할 수 있다")
    void createKrwMoney() {
        // when
        Money money = Money.krw(10000);
        
        // then
        assertThat(money.getAmount()).isEqualTo(new BigDecimal("10000.00"));
        assertThat(money.getCurrency()).isEqualTo("KRW");
    }
    
    @Test
    @DisplayName("같은 통화의 금액을 더할 수 있다")
    void addSameCurrency() {
        // given
        Money money1 = Money.krw(1000);
        Money money2 = Money.krw(2000);
        
        // when
        Money result = money1.add(money2);
        
        // then
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("3000.00"));
        assertThat(result.getCurrency()).isEqualTo("KRW");
    }
    
    @Test
    @DisplayName("다른 통화의 금액은 더할 수 없다")
    void cannotAddDifferentCurrency() {
        // given
        Money krw = Money.krw(1000);
        Money usd = Money.of(BigDecimal.valueOf(10), "USD");
        
        // when & then
        assertThatThrownBy(() -> krw.add(usd))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("서로 다른 통화입니다");
    }
    
    @Test
    @DisplayName("같은 통화의 금액을 뺄 수 있다")
    void subtractSameCurrency() {
        // given
        Money money1 = Money.krw(3000);
        Money money2 = Money.krw(1000);
        
        // when
        Money result = money1.subtract(money2);
        
        // then
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(result.getCurrency()).isEqualTo("KRW");
    }
    
    @Test
    @DisplayName("금액을 비교할 수 있다")
    void compareMoney() {
        // given
        Money money1 = Money.krw(1000);
        Money money2 = Money.krw(2000);
        
        // when & then
        assertThat(money2.isGreaterThan(money1)).isTrue();
        assertThat(money1.isGreaterThan(money2)).isFalse();
    }
    
    @Test
    @DisplayName("0원 여부를 확인할 수 있다")
    void isZero() {
        // given
        Money zeroMoney = Money.krw(0);
        Money nonZeroMoney = Money.krw(1000);
        
        // when & then
        assertThat(zeroMoney.isZero()).isTrue();
        assertThat(nonZeroMoney.isZero()).isFalse();
    }
    
    @Test
    @DisplayName("양수 여부를 확인할 수 있다")
    void isPositive() {
        // given
        Money zeroMoney = Money.krw(0);
        Money positiveMoney = Money.krw(1000);
        
        // when & then
        assertThat(positiveMoney.isPositive()).isTrue();
        assertThat(zeroMoney.isPositive()).isFalse();
    }
    
    @Test
    @DisplayName("null 금액으로는 Money를 생성할 수 없다")
    void cannotCreateWithNullAmount() {
        // when & then
        assertThatThrownBy(() -> Money.of(null, "KRW"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("금액은 null일 수 없습니다");
    }
    
    @Test
    @DisplayName("음수 금액으로는 Money를 생성할 수 없다")
    void cannotCreateWithNegativeAmount() {
        // when & then
        assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(-1000), "KRW"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("금액은 음수일 수 없습니다");
    }
    
    @Test
    @DisplayName("null 또는 빈 통화로는 Money를 생성할 수 없다")
    void cannotCreateWithInvalidCurrency() {
        // when & then
        assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(1000), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("통화는 비어있을 수 없습니다");
            
        assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(1000), ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("통화는 비어있을 수 없습니다");
            
        assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(1000), "US"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("통화는 3자리여야 합니다");
    }
}