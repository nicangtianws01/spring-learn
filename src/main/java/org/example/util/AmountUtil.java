package org.example.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AmountUtil {

    private AmountUtil() {
    }

    public static BigDecimal formatAmount(String amount) {
        BigDecimal bigDecimal = new BigDecimal(amount);
        return bigDecimal.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public static long test(Object response){
        long price = 0;
        if (response != null && BigDecimal.class.isAssignableFrom(response.getClass())) {
            price = ((BigDecimal) response).multiply(new BigDecimal("100")).longValue();
        } else {
            throw new RuntimeException("系统错误，请稍后重试");
        }
        return price;
    }
}
