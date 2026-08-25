package com.etisalatcash.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SmsParserTest {

    @Test
    public void parsesEnglishReceivedMessage() {
        Transaction t = SmsParser.parse("Etisalat",
                "You have received EGP 500.00 from 01012345678. Ref: MP24010112345678. Your new balance is EGP 1,200.00");
        assertNotNull(t);
        assertEquals(500.00, t.amount, 0.001);
        assertEquals("01012345678", t.fromPhone);
        assertEquals("MP24010112345678", t.reference);
    }

    @Test
    public void parsesArabicReceivedMessage() {
        Transaction t = SmsParser.parse("Etisalat",
                "اتصالات كاش: تم استلام مبلغ 750.50 جنيه من 01198765432. رقم العملية: 987654321");
        assertNotNull(t);
        assertEquals(750.50, t.amount, 0.001);
        assertEquals("01198765432", t.fromPhone);
        assertEquals("987654321", t.reference);
    }

    @Test
    public void parsesArabicIndicDigits() {
        Transaction t = SmsParser.parse("Etisalat",
                "تمت اضافة مبلغ ١٢٥ جنيه الى محفظتك من ٠١٠١٢٣٤٥٦٧٨");
        assertNotNull(t);
        assertEquals(125.0, t.amount, 0.001);
        assertEquals("01012345678", t.fromPhone);
    }

    @Test
    public void rejectsOutgoingOrOtpMessages() {
        assertNull(SmsParser.parse("Etisalat", "Your OTP is 483920. Do not share it with anyone."));
        assertNull(SmsParser.parse("Etisalat", "You have sent EGP 200.00 to 01099998888. Ref: ABC12345"));
    }

    @Test
    public void rejectsNonEtisalatSender() {
        assertNull(SmsParser.parse("Vodafone", "You have received EGP 100.00 from 01012345678"));
    }

    @Test
    public void amountBeforeCurrencyIsSupported() {
        Transaction t = SmsParser.parse("Etisalat", "اتصالات كاش: حول لك عميل 300 ج.م — استلام ناجح");
        assertNotNull(t);
        assertEquals(300.0, t.amount, 0.001);
    }

    @Test
    public void normalizesDigits() {
        assertEquals("0123456789.5", SmsParser.normalizeDigits("٠١٢٣٤٥٦٧٨٩٫٥"));
    }
}
