package com.etisalatcash.gateway;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Etisalat Cash and Vodafone Cash payment-confirmation SMS messages.
 * Message formats vary (Arabic/English), so patterns are kept permissive.
 */
public final class SmsParser {

    private static final Pattern AMOUNT_PATTERNS = Pattern.compile(
            "(?:EGP|e\\.gp|جنيه(?:\\s*مصري)?|ج\\s*\\.?\\s*م)\\s*[:：]?\\s*([0-9٠-٩][0-9٠-٩,]*(?:[.,][0-9٠-٩]{1,2})?)"
                    + "|([0-9٠-٩][0-9٠-٩,]*(?:[.,][0-9٠-٩]{1,2})?)\\s*(?:EGP|e\\.gp|جنيه(?:\\s*مصري)?|ج\\s*\\.?\\s*م)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("\\b(01[0-9٠-٩]{9})\\b");

    private static final Pattern REF_PATTERN = Pattern.compile(
            "(?:Ref(?:erence)?\\s*(?:No\\.?|Number)?|رقم\\s*العملية|رقم\\s*المرجع|مرجع)\\s*[:：.]?\\s*([A-Za-z0-9][A-Za-z0-9.\\-]{3,})",
            Pattern.CASE_INSENSITIVE);

    private static final String[] MONEY_IN_KEYWORDS = {
            "received", "receive", "credited", "استلام", "استلمت", "تم اضافة", "تمت اضافة",
            "تم إضافة", "حول لك", "حوّل لك", "حوالة واردة", "ايداع", "إيداع",
            "الى محفظتك", "إلى محفظتك"
    };

    private SmsParser() {}

    public static boolean isFromWallet(String sender, String body) {
        String s = sender == null ? "" : sender.toLowerCase(Locale.US);
        String b = body == null ? "" : body.toLowerCase(Locale.US);
        return s.contains("etisalat") || s.contains("vodafone")
                || b.contains("etisalat cash") || b.contains("vodafone cash")
                || b.contains("اتصالات كاش") || b.contains("اتصالات مصر")
                || b.contains("فودافون كاش") || b.contains("فودافون");
    }

    public static boolean looksLikeIncomingPayment(String body) {
        if (body == null) return false;
        String b = body.toLowerCase(Locale.US);
        for (String k : MONEY_IN_KEYWORDS) {
            if (b.contains(k.toLowerCase(Locale.US))) return true;
        }
        return false;
    }

    public static Transaction parse(String sender, String body) {
        if (!isFromWallet(sender, body) || !looksLikeIncomingPayment(body)) return null;

        String normalized = normalizeDigits(body);
        Transaction t = new Transaction();
        t.time = System.currentTimeMillis();
        t.sender = sender;
        t.rawBody = body;

        Matcher m = AMOUNT_PATTERNS.matcher(normalized);
        if (m.find()) {
            String num = m.group(1) != null ? m.group(1) : m.group(2);
            t.amount = parseAmount(num);
        } else {
            return null; // a payment message must contain an amount
        }

        Matcher p = PHONE_PATTERN.matcher(normalized);
        if (p.find()) t.fromPhone = p.group(1);

        Matcher r = REF_PATTERN.matcher(normalized);
        if (r.find()) t.reference = r.group(1).replaceAll("[.\\-]+$", "");

        return t;
    }

    static String normalizeDigits(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '٠' && c <= '٩') out.append((char) ('0' + (c - '٠')));
            else if (c == '٫') out.append('.');
            else out.append(c);
        }
        return out.toString();
    }

    private static double parseAmount(String num) {
        try {
            return Double.parseDouble(num.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
