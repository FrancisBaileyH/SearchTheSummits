package com.francisbailey.htmldate.extractor

import java.util.regex.Pattern

class DateExtractorPatterns {

    companion object {
        val LAST_NON_DIGITS: Pattern = Pattern.compile("\\D+$")

        val YMD_NO_SEP_PATTERN: Pattern = Pattern.compile("(?:\\D|^)(\\d{8})(?:\\D|\$)")
        val YMD_PATTERN: Pattern = Pattern.compile("(?:\\D|^)(\\d{4})[\\-/.](\\d{1,2})[\\-/.](\\d{1,2})(?:\\D|\$)")
        val DMY_PATTERN: Pattern = Pattern.compile("(?:\\D|^)(\\d{1,2})[\\-/.](\\d{1,2})[\\-/.](\\d{2,4})(?:\\D|\$)")
        val YM_PATTERN: Pattern = Pattern.compile("(?:\\D|^)(\\d{4})[\\-/.](\\d{1,2})(?:\\D|\$)")
        val MY_PATTERN: Pattern = Pattern.compile("(?:\\D|^)(\\d{1,2})[\\-/.](\\d{4})(?:\\D|\$)")
        val LONG_MDY_PATTERN: Pattern = Pattern.compile(
            "(January|February|March|April|May|June|July|August|September|October|November|December|" +
            "Januari|Februari|Maret|Mei|Juni|Juli|Agustus|Oktober|Desember|" +
            "Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec|" +
            "Januar|Jänner|Februar|Feber|März|Mai|Dezember|" +
            "janvier|février|mars|avril|mai|juin|juillet|aout|septembre|octobre|novembre|décembre|" +
            "Ocak|Şubat|Mart|Nisan|Mayıs|Haziran|Temmuz|Ağustos|Eylül|Ekim|Kasım|Aralık|" +
            "Oca|Şub|Mar|Nis|Haz|Tem|Ağu|Eyl|Eki|Kas|Ara)" +
            " ([0-9]{1,2})(?:st|nd|rd|th)?,? ([0-9]{2,4})"
        )
        val LONG_DMY_PATTERN: Pattern = Pattern.compile(
            "([0-9]{1,2})(?:st|nd|rd|th)? (?:of )?(" +
            "January|February|March|April|May|June|July|August|September|October|November|December|" +
            "Januari|Februari|Maret|Mei|Juni|Juli|Agustus|Oktober|Desember|" +
            "Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec|" +
            "Januar|Jänner|Februar|Feber|März|Mai|Dezember|" +
            "janvier|février|mars|avril|mai|juin|juillet|aout|septembre|octobre|novembre|décembre|" +
            "Ocak|Şubat|Mart|Nisan|Mayıs|Haziran|Temmuz|Ağustos|Eylül|Ekim|Kasım|Aralık|" +
            "Oca|Şub|Mar|Nis|Haz|Tem|Ağu|Eyl|Eki|Kas|Ara),? " +
            "([0-9]{2,4})"
        )

        val COMPLETE_URL: Pattern = Pattern.compile("(?i)([0-9]{4})[/-]([0-9]{1,2})[/-]([0-9]{1,2})")
        val PARTIAL_URL: Pattern = Pattern.compile("(?i)/([0-9]{4})[/-]([0-9]{1,2})/")

        val TIMESTAMP_PATTERN =  Pattern.compile("(?i)([0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}).[0-9]{2}:[0-9]{2}:[0-9]{2}")
        val TEXT_DATE_PATTERN = Pattern.compile("(?i)[.:,_/ \\-]|^[0-9]+\$")
        val NO_TEXT_DATE_PATTERN =  Pattern.compile("\\d{3,}\\D+\\d{3,}|\\d{2}:\\d{2}(:| )|\\+\\d{2}\\D+|\\D*\\d{4}\\D*\$")

        val ENGLISH_PATTERN = Pattern.compile("(?i)(?:date[^0-9\"]{0,20}|updated|published) *?(?:in)? *?:? *?([0-9]{1,4})[./]([0-9]{1,2})[./]([0-9]{2,4})")
        val GERMAN_PATTERN = Pattern.compile("(?i)(?:Datum|Stand): ?([0-9]{1,2})\\.([0-9]{1,2})\\.([0-9]{2,4})")
        val TURKISH_PATTERN = Pattern.compile("(?i)" +
            "(?:güncellen?me|yayı(?:m|n)lan?ma) *?(?:tarihi)? *?:? *?([0-9]{1,2})[./]([0-9]{1,2})[./]([0-9]{2,4})|" +
            "([0-9]{1,2})[./]([0-9]{1,2})[./]([0-9]{2,4}) *?(?:'de|'da|'te|'ta|’de|’da|’te|’ta|tarihinde) *(?:güncellendi|yayı(?:m|n)landı)"
        )

        val YEAR_PATTERN         = Pattern.compile("^\\D?(199[0-9]|20[0-9]{2})")
        val COPYRIGHT_PATTERN    = Pattern.compile("(?:©|\\&copy;|Copyright|\\(c\\))\\D*(?:[12][0-9]{3}-)?([12][0-9]{3})\\D")
        val THREE_PATTERN        = Pattern.compile("/([0-9]{4}/[0-9]{2}/[0-9]{2})[01/]")
        val THREE_CATCH          = Pattern.compile("([0-9]{4})/([0-9]{2})/([0-9]{2})")
        val THREE_LOOSE_PATTERN  = Pattern.compile("\\D([0-9]{4}[/.-][0-9]{2}[/.-][0-9]{2})\\D")
        val THREE_LOOSE_CATCH    = Pattern.compile("([0-9]{4})[/.-]([0-9]{2})[/.-]([0-9]{2})")
        val SELECT_YMD_PATTERN   = Pattern.compile("\\D([0-3]?[0-9][/.-][01]?[0-9][/.-][0-9]{4})\\D")
        val SELECT_YMD_YEAR      = Pattern.compile("(19[0-9]{2}|20[0-9]{2})\\D?$")
        val YMD_YEAR             = Pattern.compile("^([0-9]{4})")
        val DATE_STRINGS_PATTERN = Pattern.compile("(\\D19[0-9]{2}[01][0-9][0-3][0-9]\\D|\\D20[0-9]{2}[01][0-9][0-3][0-9]\\D)")
        val DATE_STRINGS_CATCH: Pattern  = Pattern.compile("([12][0-9]{3})([01][0-9])([0-3][0-9])")
        val SLASHES_PATTERN: Pattern     = Pattern.compile("\\D([0-3]?[0-9][/.][01]?[0-9][/.][0129][0-9])\\D")
        val SLASHES_YEAR: Pattern        = Pattern.compile("([0-9]{2})$")
        val YYYYMM_PATTERN       = Pattern.compile("\\D([12][0-9]{3}[/.-][01][0-9])\\D")
        val YYYYMM_CATCH         = Pattern.compile("([12][0-9]{3})[/.-]([01][0-9])")
        val MMYYYY_PATTERN       = Pattern.compile("\\D([01]?[0-9][/.-][12][0-9]{3})\\D")
        val MMYYYY_YEAR_PATTERN  = Pattern.compile("([12][0-9]{3})\\D?$")
        val SIMPLE_PATTERN       = Pattern.compile("\\D(199[0-9]|20[0-9]{2})\\D")

        // Time patterns
        val TZ_CODE     = Pattern.compile("(?i)(?:\\s|^)([-+])(\\d{2})(?::?(\\d{2}))?")
        val ISO_TIME    = Pattern.compile("(?i)(\\d{2}):(\\d{2})(?::(\\d{2})(?:\\.\\d+)?)?(Z|[+-]\\d{2}(?::?\\d{2})?)")
        val COMMON_TIME = Pattern.compile("(?i)(?:\\D|^)(\\d{1,2})(?::|\\s*h\\s*)(\\d{1,2})(?::(\\d{1,2})(?:\\.\\d+)?)?(?:\\s*((?:a|p)\\.?m\\.?))?")

        val MONTH_NUMBER_MAP = mapOf(
            "Januar" to  1, "Jänner" to  1, "January" to  1, "Januari" to  1, "Jan" to  1, "Ocak" to  1, "Oca" to  1, "janvier" to  1,
            "Februar" to  2, "Feber" to  2, "February" to  2, "Februari" to  2, "Feb" to  2, "Şubat" to  2, "Şub" to  2, "février" to  2,
            "März" to  3, "March" to  3, "Maret" to  3, "Mar" to  3, "Mart" to  3, "mars" to  3,
            "April" to  4, "Apr" to  4, "Nisan" to  4, "Nis" to  4, "avril" to  4,
            "Mai" to  5, "May" to  5, "Mei" to  5, "Mayıs" to  5, "mai" to  5,
            "Juni" to  6, "June" to  6, "Jun" to  6, "Haziran" to  6, "Haz" to  6, "juin" to  6,
            "Juli" to  7, "July" to  7, "Jul" to  7, "Temmuz" to  7, "Tem" to  7, "juillet" to  7,
            "August" to  8, "Agustus" to  8, "Aug" to  8, "Ağustos" to  8, "Ağu" to  8, "aout" to  8,
            "September" to  9, "Sep" to  9, "Eylül" to  9, "Eyl" to  9, "septembre" to  9,
            "Oktober" to  10, "October" to  10, "Oct" to  10, "Ekim" to  10, "Eki" to  10, "octobre" to  10,
            "November" to  11, "Nov" to  11, "Kasım" to  11, "Kas" to  11, "novembre" to  11,
            "Dezember" to  12, "December" to  12, "Desember" to  12, "Dec" to  12, "Aralık" to  12, "Ara" to  12, "décembre" to  12,
        )
    }

}