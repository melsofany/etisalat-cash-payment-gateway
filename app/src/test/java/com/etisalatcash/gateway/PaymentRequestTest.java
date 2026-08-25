package com.etisalatcash.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PaymentRequestTest {

    private PaymentRequest req(String phone, double amount) {
        PaymentRequest r = new PaymentRequest();
        r.id = "RTEST1";
        r.customerPhone = phone;
        r.amount = amount;
        r.createdAt = 1L;
        return r;
    }

    @Test
    public void matchesSamePhoneAndAmount() {
        assertTrue(req("01012345678", 150.0).matches("01012345678", 150.0));
    }

    @Test
    public void matchesInternationalFormat() {
        assertTrue(req("01012345678", 150.0).matches("+201012345678", 150.0));
        assertTrue(req("+20 101 234 5678", 150.0).matches("01012345678", 150.0));
    }

    @Test
    public void rejectsDifferentAmountOrPhone() {
        PaymentRequest r = req("01012345678", 150.0);
        assertFalse(r.matches("01012345678", 151.0));
        assertFalse(r.matches("01099998888", 150.0));
        assertFalse(r.matches(null, 150.0));
    }

    @Test
    public void rejectsAlreadyPaid() {
        PaymentRequest r = req("01012345678", 150.0);
        r.paid = true;
        assertFalse(r.matches("01012345678", 150.0));
    }

    @Test
    public void validatesEgyptianPhones() {
        assertTrue(PaymentRequest.isValidEgyptianPhone("01012345678"));
        assertTrue(PaymentRequest.isValidEgyptianPhone("+201112345678"));
        assertFalse(PaymentRequest.isValidEgyptianPhone("12345"));
        assertFalse(PaymentRequest.isValidEgyptianPhone("02012345678"));
    }

    @Test
    public void messageContainsAmountMerchantAndId() {
        PaymentRequest r = req("01012345678", 250.5);
        r.note = "فاتورة 99";
        String msg = r.buildMessage("01098765432");
        assertTrue(msg.contains("250.50"));
        assertTrue(msg.contains("01098765432"));
        assertTrue(msg.contains("RTEST1"));
        assertTrue(msg.contains("فاتورة 99"));
    }

    @Test
    public void idsAreUnique() {
        assertFalse(PaymentRequest.newId().equals(PaymentRequest.newId()));
    }
}
