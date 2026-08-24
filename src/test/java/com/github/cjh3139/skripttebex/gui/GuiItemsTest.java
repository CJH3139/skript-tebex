package com.github.cjh3139.skripttebex.gui;

import com.github.cjh3139.skripttebex.api.model.Package;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuiItemsTest {
    private static Package pkg(double base, double total, double discount) {
        return new Package(1, "VIP", "<p>Cool <b>rank</b></p>", null, "single",
            base, total, "USD", discount, "vip", 0);
    }

    @Test
    void stripsHtmlToReadableText() {
        assertEquals("Cool rank", GuiItems.stripHtml("<p>Cool <b>rank</b></p>"));
        assertEquals("Line one\nLine two", GuiItems.stripHtml("Line one<br>Line two"));
        assertEquals("", GuiItems.stripHtml(null));
    }

    @Test
    void formatsPriceWithAndWithoutDiscount() {
        assertEquals("9.99 USD", GuiItems.priceLabel(pkg(9.99, 9.99, 0.0)));
        assertEquals("9.99 USD (was 12.99)", GuiItems.priceLabel(pkg(12.99, 9.99, 3.0)));
    }

    @Test
    void paginatesAndClampsOutOfRange() {
        List<Integer> items = List.of(1, 2, 3, 4, 5);

        assertEquals(List.of(1, 2), GuiItems.paginate(items, 0, 2));
        assertEquals(List.of(5), GuiItems.paginate(items, 2, 2));
        assertEquals(List.of(), GuiItems.paginate(items, 9, 2));
    }
}
